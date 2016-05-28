package edu.emory.mathcs.ir.liveqa.parsing

import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate._
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import edu.emory.mathcs.ir.liveqa.util.HtmlScraper
import edu.emory.mathcs.ir.liveqa.verticals.web.ContentExtractor
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import org.joda.time.DateTime

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

/**
  * Parses TREC qrel file format and returns an array of questions with their
  * rated answer candidates.
  */
object QrelParser extends LazyLogging {

  def getSource(source: String): AnswerType = source match {
    case src if src.contains("answers.yahoo.com") || src.contains("WEBSCOPE") => YAHOO_ANSWERS
    case src if src.contains("webmd.com") => WEBMD
    case src if src.contains("answers.com") => ANSWERS_COM
    case src if src.contains("ehow.com") => EHOW
    case src if src.contains("wikihow.com") => WIKIHOW
    case _ => WEB
  }

  def addAttributes(candidate: AnswerCandidate, text: String, source: String): Unit = {
    val browser = JsoupBrowser()
    try {
      val content = HtmlScraper.apply(source)
      content.foreach {
        contentOption =>
          contentOption.foreach {
            content =>
              val document = browser.parseString(content)
              candidate.attributes(QuestionTitle) = document.title
              val contentBlocks = ContentExtractor(content)
              var prevBlock = ""
              for (block <- contentBlocks) {
                val textWords = text.replaceAll("[^A-Za-z0-9\\s]", "").toLowerCase.split("\\s").toSet
                val blockWords = block.substring(0, math.min(block.length, 1000)).replaceAll("[^A-Za-z0-9 ]", "").toLowerCase.split("\\s").toSet

                if (textWords.intersect(blockWords).size > 0.75 * textWords.size) {
                  candidate.attributes(QuestionBody) = prevBlock
                }
                prevBlock = block
              }
          }
      }
    } catch {
      case exc:Exception => logger.error(exc.getMessage)
    }
  }

  def apply(src: Source): Seq[(Question, Seq[AnswerCandidate])] = {
    val questions = new mutable.MutableList[(Question, mutable.MutableList[AnswerCandidate])]
    for (line <- src.getLines()) {
      line.split("\t", -1) match {
        case Array(_, qid, rel, title, body, cat, mainCat) if rel.isEmpty =>
          questions += ((new Question(qid, mainCat, title, Option(body), DateTime.now()),
            new mutable.MutableList[AnswerCandidate]))
        case Array(_, qid, rel, text, source, _, _) =>
          questions.last._2 += new AnswerCandidate(getSource(source), text, source)
          questions.last._2.last.attributes(Relevance) = rel
          addAttributes(questions.last._2.last, text, source)
      }
    }
    questions.map {
      case (question, candidates) => (question, Random.shuffle(candidates))
    }
  }
}
