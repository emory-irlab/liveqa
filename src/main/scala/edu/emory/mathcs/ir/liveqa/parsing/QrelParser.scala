package edu.emory.mathcs.ir.liveqa.parsing

import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate._
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import org.joda.time.DateTime

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

/**
  * Parses TREC qrel file format and returns an array of questions with their
  * rated answer candidates.
  */
class QrelParser(docsLocation: String) extends LazyLogging {
  val docIndexes =
    scala.io.Source.fromFile(docsLocation + "urls.txt").getLines
      .zipWithIndex.map(ui => ui._1 -> (ui._2 + 5)).toMap

  def getSource(source: String): AnswerType = source match {
    case src if src.contains("answers.yahoo.com") || src.contains("WEBSCOPE") => YAHOO_ANSWERS
    case src if src.contains("webmd.com") => WEBMD
    case src if src.contains("answers.com") => ANSWERS_COM
    case src if src.contains("ehow.com") => EHOW
    case src if src.contains("wikihow.com") => WIKIHOW
    case _ => WEB
  }

  def addAttributes(candidate: AnswerCandidate, text: String, source: String): Unit = {
    try {
      if (source.startsWith("http")) {
        val url = source.split(";")(0)
        if (docIndexes.contains(url)) {
          val src = scala.io.Source.fromFile(docsLocation + docIndexes(url) + "_content.txt")
          val content = src.mkString.split("\n")
          src.close()
          if (url.contains("answers.yahoo.com")) {
            val qid = content(0)
            val category = content(1)
            val title = content(2)
            val body = content(3)
            candidate.attributes(QuestionTitle) = title
            candidate.attributes(QuestionBody) = body
            candidate.attributes(QuestionCategories) = category
          } else {
            candidate.attributes(QuestionTitle) = content(0)
            candidate.attributes(QuestionBody) = if (content.length > 1) content(1) else ""
            var prevBlock = ""
            for (block <- content.iterator.drop(1)) {
              val textWords = text.replaceAll("[^A-Za-z0-9\\s]", "").toLowerCase.split("\\s").toSet
              val blockWords = block.substring(0, math.min(block.length, 1000)).replaceAll("[^A-Za-z0-9 ]", "").toLowerCase.split("\\s").toSet

              if (textWords.intersect(blockWords).size > 0.75 * textWords.size) {
                candidate.attributes(QuestionBody) = prevBlock
              }
              prevBlock = block
            }
          }
        }
      }
    } catch {
      case exc:Exception =>
        logger.error(exc.getMessage)
    }
  }

  def apply(src: Source): Seq[(Question, Seq[AnswerCandidate])] = {
    val questions = new mutable.MutableList[(Question, mutable.MutableList[AnswerCandidate])]

    for (line <- src.getLines()) {
      line.split("\t", -1) match {
        case Array(_, qid, rel, title, body, cat, mainCat) if rel.isEmpty =>
          questions += ((new Question(qid, mainCat, title, Option(body), DateTime.now()),
            new mutable.MutableList[AnswerCandidate]))
        case Array(_, qid, rel, answerText, source, _, _) =>
          questions.last._2 += new AnswerCandidate(getSource(source), answerText, source)
          questions.last._2.last.attributes(Relevance) = rel
          addAttributes(questions.last._2.last, answerText, source)
      }
    }

    questions.map {
      case (question, candidates) => (question, Random.shuffle(candidates))
    }
  }
}
