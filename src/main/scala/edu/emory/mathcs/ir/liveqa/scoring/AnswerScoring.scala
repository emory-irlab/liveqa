package edu.emory.mathcs.ir.liveqa.scoring

import edu.emory.mathcs.ir.liveqa.{AnswerCandidate, Question}

/**
  * Scoring a candidate answer for the given question.
  */
trait AnswerScoring {
  /**
    * Returns a score for the given candidate answer.
    * @param question A question to score candidate answer to.
    * @param answer Candidate answer to score.
    * @return A floating point score for the given answer.
    */
  def apply(question: Question, answer: AnswerCandidate): Float
}
