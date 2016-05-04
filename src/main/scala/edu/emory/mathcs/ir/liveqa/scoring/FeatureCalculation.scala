package edu.emory.mathcs.ir.liveqa.scoring

import edu.emory.mathcs.ir.liveqa.base.{Question, AnswerCandidate}

/**
  * Inteface for computing answer candidate features.
  */
trait FeatureCalculation {
  /**
    * Compute a set of features for the given answer candidate.
    * @param question Current question, for which the candidate was generated.
    * @param answer Answer candidate to compute features for.
    * @return A map from feature names to the corresponding values.
    */
  def computeFeatures(question: Question, answer: AnswerCandidate) : Map[String, Float]
}
