package edu.emory.mathcs.ir.liveqa.scoring

import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import scala.collection.JavaConverters._

/**
  * Scores the answer by the term overlap between the question and answer texts.
  */
class TermOverlapAnswerScorer extends AnswerScoring {
  /**
    * Returns a score for the given candidate answer.
    *
    * @param question A question to score candidate answer to.
    * @param answer   Candidate answer to score.
    * @return A floating point score for the given answer.
    */
  override def score(question: Question, answer: AnswerCandidate): Double = {
    val titleTerms = question.titleNlp.sentences().asScala.flatMap(s => s.lemmas.asScala).toSet
    val bodyTerms = question.bodyNlp.sentences().asScala.flatMap(s => s.lemmas.asScala).toSet
    val answerTerms = answer.textNlp.sentences().asScala.flatMap(s => s.lemmas.asScala).toSet

    // Return 0.8 title overlap + 0.2 body overlap.
    0.8 * titleTerms.intersect(answerTerms).size + 0.2 * bodyTerms.intersect(answerTerms).size
  }
}
