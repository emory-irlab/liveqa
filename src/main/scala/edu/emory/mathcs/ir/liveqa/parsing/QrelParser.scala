package edu.emory.mathcs.ir.liveqa.parsing

import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.{Relevance, WEB}
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import org.joda.time.DateTime

import scala.collection.mutable
import scala.io.Source

/**
  * Parses TREC qrel file format and returns an array of questions with their
  * rated answer candidates.
  */
object QrelParser {
  def apply(src: Source): Seq[(Question, Seq[AnswerCandidate])] = {
    val questions = new mutable.MutableList[(Question, mutable.MutableList[AnswerCandidate])]
    for (line <- src.getLines()) {
      line.split("\t", -1) match {
        case Array(_, qid, rel, title, body, cat, mainCat) if rel.isEmpty =>
          questions += ((new Question(qid, mainCat, title, Option(body), DateTime.now()),
            new mutable.MutableList[AnswerCandidate]))
        case Array(_, qid, rel, text, source, _, _) =>
          questions.last._2 += new AnswerCandidate(WEB, text, source)
          questions.last._2.last.attributes(Relevance) = rel
      }
    }
    questions
  }
}
