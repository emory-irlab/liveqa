package edu.emory.mathcs.ir.liveqa.base

import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.{CandidateAttribute, AnswerType}
import edu.stanford.nlp.simple.Document
import CandidateAnswerTextProcessor._

/**
  * A candidate answer, extracted from somewhere on the web.
  */
class AnswerCandidate(val answerType: AnswerType,
                      txt: String,
                      val source: String)(implicit textProcessor: CandidateAnswerTextProcessing = AnswerTextMaxlenCutter) {
  val attributes = new scala.collection.mutable.HashMap[CandidateAttribute, String]
  private val attributesNlp = new scala.collection.mutable.HashMap[CandidateAttribute, Document]
  val features = new scala.collection.mutable.HashMap[String, Float]
  val text = textProcessor(txt)
  val textNlp = new Document(text)

  /**
    * Returns Stanford CoreNLP [[Document]] object for the given attribute.
    * @param attr The attribute to get.
    * @return [[Document]] object that can be used for NLP analysis of the
    *        attribute text.
    */
  def getAttributeNlp(attr: CandidateAttribute): Option[Document] = {
    if (attributes.contains(attr))
      Some(attributesNlp.getOrElseUpdate(attr, new Document(attributes.getOrElse(attr, ""))))
    else
      None
  }

  override def toString = s"$answerType\t${text.replace("\n", " ").replace("\t", " ")}\t${source.replace("\n", " ")}"
}

object AnswerCandidate {
  trait AnswerType
  case object YAHOO_ANSWERS extends AnswerType
  case object ANSWERS_COM extends AnswerType
  case object WEB extends AnswerType
  case object WEBMD extends AnswerType
  case object EHOW extends AnswerType
  case object WIKIHOW extends AnswerType

  trait CandidateAttribute
  case object QuestionTitle extends CandidateAttribute
  case object QuestionBody extends CandidateAttribute
  case object QuestionMainCategory extends CandidateAttribute
  case object QuestionCategories extends CandidateAttribute
  case object QuestionId extends CandidateAttribute
  // The rank the of the answer in the original source, e.g. web page rank.
  case object CandidateSourceRank extends CandidateAttribute
  case object Id extends CandidateAttribute
  case object Relevance extends CandidateAttribute
  // Rank of the candidate according to some ranking function.
  case object CandidateRank extends CandidateAttribute
}