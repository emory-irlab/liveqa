package edu.emory.mathcs.ir.liveqa.scoring.features

import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}

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

/**
  * Feature generator, that combines features produced by the provided list
  * of feature generators.
  * @param features A list of feature generators to apply and merge.
  */
class MergeFeatures(features: FeatureCalculation*) extends FeatureCalculation {
  /**
    * Compute a set of features for the given answer candidate.
    *
    * @param question Current question, for which the candidate was generated.
    * @param answer   Answer candidate to compute features for.
    * @return A map from feature names to the corresponding values.
    */
  override def computeFeatures(question: Question, answer: AnswerCandidate): Map[String, Float] = {
    features.flatMap(_.computeFeatures(question, answer)).toMap
  }
}