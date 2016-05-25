package edu.emory.mathcs.ir.liveqa.ranking

import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.Relevance
import edu.emory.mathcs.ir.liveqa.scoring.AnswerScoring

/**
  * A trait to rank answer candidates.
  */
trait AnswerRanking {
  /**
    * Ranks a set of candidate answers according to some criteria.
    * @param question Question to which to rank the candidates.
    * @param candidates Answer candidates to rank.
    * @return A ranked list of candidate answers.
    */
  def rank(question: Question, candidates: Seq[AnswerCandidate])
      : Seq[AnswerCandidate]
}

/**
  * Dummy ranker that returns candidates in the same order.
  */
class DummyRanking extends AnswerRanking {
  override def rank(question: Question, candidates: Seq[AnswerCandidate])
      : Seq[AnswerCandidate] = {
    candidates
  }
}

/**
  * A ranker that ranks candidates by their relevance if present as one of the
  * attributes. Useful for debugging purposes.
  */
class RelevanceRanking extends AnswerRanking {
  override def rank(question: Question, candidates: Seq[AnswerCandidate])
      : Seq[AnswerCandidate] = {
    assert(candidates.forall(_.attributes.contains(Relevance)))
    candidates.sorted(
      Ordering.by((_: AnswerCandidate).attributes.get(Relevance).get.toDouble)
        .reverse)
  }
}

/**
  * Ranker that uses the provided scorer to generates scores of candidates and
  * uses these scores for ranking.
  * @param scorer [[AnswerScoring]] to use for ranking.
  */
class ScoringBasedRanking(scorer: AnswerScoring) extends AnswerRanking {
  /**
    * Ranks a set of candidate answers based on the scorer scores.
    *
    * @param candidates Answer candidates to rank.
    * @return A ranked list of candidate answers.
    */
  override def rank(question: Question, candidates: Seq[AnswerCandidate])
      : Seq[AnswerCandidate] = {
    candidates.sorted(Ordering.by(scorer.score(question, _: AnswerCandidate)).reverse)
  }
}