package edu.emory.mathcs.ir.liveqa.base

import com.twitter.util.Future

/**
  * A train to generate candidate answers for the given question.
  */
trait CandidateGeneration {
  def getCandidateAnswers(question: Question) : Future[Seq[AnswerCandidate]]
}
