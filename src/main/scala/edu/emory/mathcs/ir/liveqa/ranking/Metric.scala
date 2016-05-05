package edu.emory.mathcs.ir.liveqa.ranking

import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.Relevance

/**
  * Created by dsavenk on 5/5/16.
  */
trait Metric {
  /**
    * Computes a metric for the given answer ranking.
    * @param ranking A list of ranked [[AnswerCandidate]] objects, which should
    *                contain [[edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.Relevance]]
    *                annotation with the relevance score.
    * @return A score for the given ranking.
    */
  def compute(ranking: Seq[AnswerCandidate]): Double = {
    assert(ranking.forall(_.attributes.contains(Relevance)))
    assert(ranking.nonEmpty)
    0
  }
}

/**
  * A class that computes Precision@k metric.
  * @param k The number of top ranked documents to score.
  * @param threshold A minimum relevance score for the [[AnswerCandidate]] to
  *                  be considered relevant.
  */
class PrecisionAtK(k: Int, threshold: Double = 2) extends Metric {
  /**
    * Computes a metric for the given answer ranking.
    *
    * @param ranking A list of ranked [[AnswerCandidate]] objects, which should
    *                contain [[edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.Relevance]]
    *                annotation with the relevance score.
    * @return A score for the given ranking.
    */
  override def compute(ranking: Seq[AnswerCandidate]): Double = {
    super.compute(ranking)
    val relCount =
      ranking.take(k).map(candidate =>
          (if (candidate.attributes.get(Relevance).get.toDouble >= threshold)
            1.0
          else
            0.0, 1))
        .reduce((prev, cur) => (prev._1 + cur._1, prev._2 + cur._2))
    relCount._1 / relCount._2
  }
}

class AveragePrecision(k:Int, threshold: Double = 2.0) extends Metric {
  override def compute(ranking: Seq[AnswerCandidate]): Double = {
    super.compute(ranking)
    val relevance = ranking.take(k).scanLeft(0.0)((curRel, candidate) =>
      if (candidate.attributes.get(Relevance).get.toDouble >= threshold)
        curRel + 1.0 else curRel)
    relevance.zipWithIndex.filter(_._1 > 0.0)
      .foldLeft(0.0)((prec, relWithIndex) =>
        prec + relWithIndex._1 / relWithIndex._2) / math.min(ranking.size, k)
  }
}

/**
  * Computes Discounted Cummulative Gain ranking metric.
  * @param k Top K documents to consider.
  */
class Dcg(k: Int) extends Metric {
  override def compute(ranking: Seq[AnswerCandidate]): Double = {
    super.compute(ranking)
    ranking.take(k).map(_.attributes.get(Relevance).get.toDouble).zipWithIndex
      .foldLeft(0.0)((res, relWithIndex) =>
        res +
          (math.pow(2, relWithIndex._1) - 1) / math.log(relWithIndex._2 + 2))
  }
}

/**
  * Computes normalized Discounted Cummulative Gain IR metric.
  * @param k Top K documents to consider.
  */
class Ndcg(k: Int) extends Dcg(k) {
  override def compute(ranking: Seq[AnswerCandidate]): Double = {
    val dcg = super.compute(ranking)
    val idealRanking = ranking.sorted(
      Ordering.by((_: AnswerCandidate).attributes.get(Relevance).get.toDouble)
        .reverse)
    val idcg = super.compute(idealRanking)
    if (idcg > 0) dcg / idcg else 0.0
  }
}

/**
  * Computes an average of a ranking metric over all the given questions.
  * @param metric [[Metric]] to apply to each ranking.
  */
class AverageRankingMetric(metric: Metric,
                           ranking: AnswerRanking = new DummyRanking) {
  def compute(questions: Seq[(Question, Seq[AnswerCandidate])]): Double = {
    questions.map(questionAnswers =>
        metric.compute(ranking.rank(questionAnswers._1, questionAnswers._2)))
      .sum / questions.size
  }
}