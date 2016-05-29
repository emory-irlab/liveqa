package edu.emory.mathcs.ir.liveqa.scoring.features

import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate._
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}

/**
  * Created by dsavenk on 5/29/16.
  */
class SourceFeatures extends FeatureCalculation {
  /**
    * Compute a set of features for the given answer candidate.
    *
    * @param question Current question, for which the candidate was generated.
    * @param answer   Answer candidate to compute features for.
    * @return A map from feature names to the corresponding values.
    */
  override def computeFeatures(question: Question,
                               answer: AnswerCandidate): Map[String, Float] = {
    Map[String, Float](answer.answerType match {
      case WEB => "SourceWeb" -> 1f
      case YAHOO_ANSWERS => "SourceYahooAnswers" -> 1f
      case ANSWERS_COM => "SourceAnswers" -> 1f
      case EHOW => "SourceEhow" -> 1f
      case WEBMD => "SourceWebmd" -> 1f
      case WIKIHOW => "SourceWikihow" -> 1f
      case _ => "Other" -> 1f
    })
  }
}
