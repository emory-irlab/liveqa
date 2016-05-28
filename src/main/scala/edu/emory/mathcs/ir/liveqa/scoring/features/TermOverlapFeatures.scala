package edu.emory.mathcs.ir.liveqa.scoring.features

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
    val titleTerms = NlpUtils.getLemmas(question.titleNlp).toSet
    val titleBigrams = NlpUtils.getLemmas(question.titleNlp).sliding(2).map(_.toList.mkString("-")).toSet
    val bodyTerms = NlpUtils.getLemmas(question.bodyNlp).toSet
    val bodyBigrams = NlpUtils.getLemmas(question.bodyNlp).sliding(2).map(_.toList.mkString("-")).toSet
    val answerTerms = NlpUtils.getLemmas(answer.textNlp).toSet
    val answerBigrams = NlpUtils.getLemmas(answer.textNlp).sliding(2).map(_.toList.mkString("-")).toSet

    Map(
      "TitleAnswerOverlap" -> titleTerms.intersect(answerTerms).size,
      "BodyAnswerOverlap" -> bodyTerms.intersect(answerTerms).size,
      "TitleAnswerOverlapNoStop" -> titleTerms.intersect(answerTerms.filter(Stopwords.not)).size,
      "BodyAnswerOverlapNoStop" -> bodyTerms.intersect(answerTerms.filter(Stopwords.not)).size,
      "TitleAnswerOverlapBigram" -> titleBigrams.intersect(answerBigrams).size,
      "BodyAnswerOverlapBigram" -> bodyBigrams.intersect(answerBigrams).size
    )
  }
}
