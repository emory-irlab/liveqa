package edu.emory.mathcs.ir.liveqa

import com.twitter.util.Future

/**
  * Candidate generator that combines multiple strategies into a single pool.
  */
class MergingCandidateGenerator(generators: CandidateGeneration*)
  extends CandidateGeneration {

  override def getCandidateAnswers(question: Question)
      : Future[Seq[AnswerCandidate]] = {

    // Get candidate answers from multiple sources and then put them together.
    Future.collect(generators.map(_.getCandidateAnswers(question)))
      .map(_.flatten)
  }
}
