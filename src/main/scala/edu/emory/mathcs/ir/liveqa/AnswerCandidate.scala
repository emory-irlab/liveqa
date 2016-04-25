package edu.emory.mathcs.ir.liveqa

import edu.emory.mathcs.ir.liveqa.AnswerCandidate.AnswerType

/**
  * A candidate answer, extracted from somewhere on the web.
  */
class AnswerCandidate(val answerType: AnswerType,
                      val text: String,
                      val source: String) {
  val attributes = new scala.collection.mutable.HashMap[String, String]

  override def toString = s"$text\n\n$source"
}

object AnswerCandidate {
  sealed trait AnswerType
  case object YAHOO_ANSWERS extends AnswerType
  case object WEB extends AnswerType
}