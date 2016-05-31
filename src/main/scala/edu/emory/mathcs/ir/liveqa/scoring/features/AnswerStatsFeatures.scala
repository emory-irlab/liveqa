package edu.emory.mathcs.ir.liveqa.scoring.features

import collection.JavaConverters._
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}

/**
  * Created by dsavenk on 5/27/16.
  */
class AnswerStatsFeatures extends FeatureCalculation {
  /**
    * Compute a set of features for the given answer candidate.
    *
    * @param question Current question, for which the candidate was generated.
    * @param answer   Answer candidate to compute features for.
    * @return A map from feature names to the corresponding values.
    */
  override def computeFeatures(question: Question, answer: AnswerCandidate): Map[String, Float] = {
    if (answer.text.isEmpty) Map[String, Float]()
    else {
      val sentences = answer.textNlp.sentences.size
      val words = answer.textNlp.sentences.asScala.flatMap(w => w.lemmas.asScala).length
      Map(
        "AnswerLengthChars" -> answer.text.length,
        "AnswerLengthSentences" -> sentences,
        "AnswerLengthWords" -> words,
        "AnswerLengthWordsPerSentence" -> (if (sentences > 0) words.toFloat / sentences else 0.0f),
        "NonAlphaChars" -> answer.text.count(!_.isLetterOrDigit).toFloat / answer.text.length,
        "QuestionMarksCount" -> answer.text.count(_ == '?').toFloat,
        "VerbsPerWord" -> (
          if (words == 0) 0
          else answer.textNlp.sentences().asScala
            .flatMap(s => s.posTags().asScala).count(_.startsWith("V")).toFloat / words
        )
      )
    }
  }
}
