package edu.emory.mathcs.ir.liveqa.tools

import java.io.FileOutputStream

import ciir.umass.edu.learning.tree.LambdaMART

import scala.pickling.Defaults._
import scala.pickling.binary._
import collection.JavaConverters._
import ciir.umass.edu.learning.{DataPoint, RANKER_TYPE, RankerTrainer}
import ciir.umass.edu.metric.NDCGScorer
import edu.emory.mathcs.ir.liveqa.parsing.QrelParser
import edu.emory.mathcs.ir.liveqa.ranking.ranklib.Converter
import edu.emory.mathcs.ir.liveqa.scoring.features._

/**
  * An app to evaluate answer ranker.
  */
object TrainApp extends App {
  val qrels = QrelParser(scala.io.Source.fromFile(args(0)))
  val qrelsVal = QrelParser(scala.io.Source.fromFile(args(1)))
  val alphabet: collection.mutable.Map[String, Int] = new collection.mutable.HashMap[String, Int]

  val featureGenerator = new MergeFeatures(
    new Bm25Features, new AnswerStatsFeatures, new TermOverlapFeatures, new MatchesFeatures
  )
  qrels.foreach {
    case (question, candidates) =>
      candidates.foreach(c => featureGenerator.computeFeatures(question, c).map(e => c.features += e))
  }
  qrelsVal.foreach {
    case (question, candidates) =>
      candidates.foreach(c => featureGenerator.computeFeatures(question, c).map(e => c.features += e))
  }

  val samples = Converter.createDataset(qrels, alphabet)
  val samplesValidation = Converter.createDataset(qrelsVal, alphabet)
  val trainer = new RankerTrainer
  val features = Array.fill(DataPoint.getFeatureCount){0}
  for (i <- features.indices) features(i) = i + 1

  val ranker = trainer.train(RANKER_TYPE.LAMBDAMART, samples.asJava, samplesValidation.asJava, features, new NDCGScorer)

  ranker.save(args(2))
  val alphabetStream = new FileOutputStream(args(3))
  alphabet.pickleTo(alphabetStream)
  alphabetStream.close()
}
