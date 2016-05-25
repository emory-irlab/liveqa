package edu.emory.mathcs.ir.liveqa.base

import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.{CandidateAttribute, AnswerType}
import edu.stanford.nlp.simple.Document



/**
  * A candidate answer, extracted from somewhere on the web.
  */
class AnswerCandidate(val answerType: AnswerType,
                      val text: String,
                      val source: String) {
  val attributes = new scala.collection.mutable.HashMap[CandidateAttribute, String]
  val features = new scala.collection.mutable.HashMap[String, Double]
  val textNlp = new Document(text)

  override def toString = s"$text\n\n$source"
}

object AnswerCandidate {
  trait AnswerType
  case object YAHOO_ANSWERS extends AnswerType
  case object ANSWERS_COM extends AnswerType
  case object WEB extends AnswerType

  trait CandidateAttribute
  case object QuestionTitle extends CandidateAttribute
  case object QuestionBody extends CandidateAttribute
  case object QuestionMainCategory extends CandidateAttribute
  case object QuestionCategories extends CandidateAttribute
  case object AnswerRank extends CandidateAttribute
  case object Id extends CandidateAttribute
  case object Relevance extends CandidateAttribute
}