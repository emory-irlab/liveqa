package edu.emory.mathcs.ir.liveqa.utils

import edu.emory.mathcs.ir.liveqa.parsing.QrelParser

/**
  * An app to evaluate answer ranker.
  */
object EvalApp extends App {
  val qrels = QrelParser(scala.io.Source.fromFile(args(0)))
  println(qrels.size)
}
