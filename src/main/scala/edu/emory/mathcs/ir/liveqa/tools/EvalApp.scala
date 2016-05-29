package edu.emory.mathcs.ir.liveqa.tools

import scala.pickling.Defaults._
import scala.pickling.binary._
import ciir.umass.edu.learning.RankerFactory
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import edu.emory.mathcs.ir.liveqa.parsing.QrelParser
import edu.emory.mathcs.ir.liveqa.ranking._
import edu.emory.mathcs.ir.liveqa.scoring.TermOverlapAnswerScorer
import edu.emory.mathcs.ir.liveqa.scoring.features._
import edu.emory.mathcs.ir.liveqa.util.HtmlScraper

import scala.pickling.Defaults._
import scala.pickling.binary._
import scala.util.Random

/**
  * An app to evaluate answer ranker.
  */
object EvalApp extends App {
  val qrels = QrelParser(scala.io.Source.fromFile(args(0)))

  val map = new AverageRankingMetric(new AveragePrecision(10))
  val ndcg = new AverageRankingMetric(new Ndcg(10))
  val p1 = new AverageRankingMetric(new PrecisionAtK(1))
  val p10 = new AverageRankingMetric(new PrecisionAtK(10))

  val featureGenerator = new MergeFeatures(
    new Bm25Features, new AnswerStatsFeatures, new TermOverlapFeatures, new MatchesFeatures
  )

  val rankers = Array(new DummyRanking,
    new RandomRanking(42),
    new RandomRanking(12345),
    new RelevanceRanking,
    new ScoringBasedRanking(new TermOverlapAnswerScorer),
    RanklibModelRanker.create(args(1), featureGenerator, args(2)))

  for (ranker <- rankers) {
    val rankedQrels = qrels.map {
      case (q, a) => (q, ranker.rank(q, a))
    }

    println(qrels.size + " test questions")
    println("P@1 = " + p1.compute(rankedQrels))
    println("P@10 = " + p10.compute(rankedQrels))
    println("MAP@10 = " + map.compute(rankedQrels))
    println("NDCG@10 = " + ndcg.compute(rankedQrels))
    println("---------------------------------------")
  }

  HtmlScraper.shutdown()
}
