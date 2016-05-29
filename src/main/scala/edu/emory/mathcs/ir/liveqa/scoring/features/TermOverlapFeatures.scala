package edu.emory.mathcs.ir.liveqa.scoring.features

import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.{QuestionBody, QuestionTitle}
import edu.stanford.nlp.simple.Document

import collection.JavaConverters._
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import edu.emory.mathcs.ir.liveqa.util.{NlpUtils, Stopwords}

/**
  * Created by dsavenk on 5/27/16.
  */
class TermOverlapFeatures extends FeatureCalculation {
  /**
    * Compute a set of features for the given answer candidate.
    *
    * @param question Current question, for which the candidate was generated.
    * @param answer   Answer candidate to compute features for.
    * @return A map from feature names to the corresponding values.
    */
  override def computeFeatures(question: Question, answer: AnswerCandidate): Map[String, Float] = {
    var feats = getOverlapFeatures(question.titleNlp, answer.textNlp, "TitleAnswer") ++
    getOverlapFeatures(question.bodyNlp, answer.textNlp, "BodyAnswer")

    if (answer.attributes.contains(QuestionTitle)) {
      feats ++= getOverlapFeatures(question.titleNlp, answer.getAttributeNlp(QuestionTitle).get, "TitleQTAnswer")
      feats ++= getOverlapFeatures(question.bodyNlp, answer.getAttributeNlp(QuestionTitle).get, "BodyQTAnswer")
    }
    if (answer.attributes.contains(QuestionBody)) {
      feats ++= getOverlapFeatures(question.titleNlp, answer.getAttributeNlp(QuestionBody).get, "TitleQTBody")
      feats ++= getOverlapFeatures(question.bodyNlp, answer.getAttributeNlp(QuestionBody).get, "BodyQTBody")
    }

    feats
  }

  def getNgrams(doc: Document, n: Int, withStopwords: Boolean): Set[String] = {
      NlpUtils.getLemmas(doc).filter(withStopwords || Stopwords.not(_))
        .sliding(n).map(_.toList.mkString("-")).toSet
  }

  def getOverlapFeatures(questionDocument: Document,
                         answerDocument: Document,
                         suffix: String,
                         n: Int = 3): Map[String, Float] = {
    (1 to n).flatMap(i => Array(true, false).map { withStop =>
      val questionNgrams = getNgrams(questionDocument, i, withStop)
      val answerNgrams = getNgrams(questionDocument, i, withStop)
      val withStopStr = if (withStop) "" else "NoStop"
      i + "-gramOverlap" + withStopStr + suffix -> questionNgrams.intersect(answerNgrams).size.toFloat
    }).toMap
  }
}
