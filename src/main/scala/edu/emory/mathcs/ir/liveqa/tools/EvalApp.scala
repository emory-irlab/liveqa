package edu.emory.mathcs.ir.liveqa.tools

import edu.emory.mathcs.ir.liveqa.parsing.QrelParser
import edu.emory.mathcs.ir.liveqa.ranking._

/**
  * An app to evaluate answer ranker.
  */
object EvalApp extends App {
  val qrels = QrelParser(scala.io.Source.fromFile(args(0)))
  val metric = new AverageRankingMetric(new AveragePrecision(10))
  println(metric.compute(qrels))
  println(qrels.size)
}
