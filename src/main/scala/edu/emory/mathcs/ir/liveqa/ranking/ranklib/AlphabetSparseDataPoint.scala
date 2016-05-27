package edu.emory.mathcs.ir.liveqa.ranking.ranklib

import ciir.umass.edu.learning._
import ciir.umass.edu.learning.boosting.AdaRank
import ciir.umass.edu.metric.PrecisionScorer

import collection.{breakOut, immutable, mutable}
import collection.JavaConverters._

/**
  * Created by dsavenk on 5/26/16.
  */
class AlphabetSparseDataPoint(pointLabel: Float,
                              questionId: String,
                              features: Map[String, Float],
                              alphabet: scala.collection.mutable.Map[String, Int]) extends DataPointEx {

  label = pointLabel
  id = questionId

  val feats: collection.mutable.Map[Int, Float] = features.map {
    case (name, value) =>
      val featureIndex = alphabet.getOrElseUpdate(name, alphabet.size)
      super.setFeatureCount(alphabet.size)
      (featureIndex + 1) -> value
  }(breakOut)

  override def getFeatureVector: Array[Float] = {
    throw new NotImplementedError("This method isn't implemented")
  }

  override def getFeatureValue(i: Int): Float = {
    feats(i)
  }

  override def setFeatureValue(i: Int, v: Float): Unit = {
    feats(i) = v
  }

  override def setFeatureVector(floats: Array[Float]): Unit = {
    throw new NotImplementedError("This method isn't implemented")
  }
}

object AlphabetSparseDataPoint extends App {
  val alphabet = new mutable.HashMap[String, Int]
  val d1: DataPoint = new AlphabetSparseDataPoint(1, "1", Map("feat1" -> -1.0f, "feat2" -> 2.0f), alphabet)
  val d2: DataPoint = new AlphabetSparseDataPoint(2, "1", Map("feat1" -> 1.0f, "feat2" -> 3.0f), alphabet)
  val d3: DataPoint = new AlphabetSparseDataPoint(3, "1", Map("feat1" -> -1.0f, "feat2" -> 5.0f), alphabet)
  val d4: DataPoint = new AlphabetSparseDataPoint(0, "1", Map("feat1" -> 1.0f, "feat2" -> 1.0f, "feat3" -> -0.4f), alphabet)
  val d5: DataPoint = new AlphabetSparseDataPoint(4, "1", Map("feat1" -> 1.0f, "feat2" -> 5.5f, "feat3" -> 0.4f), alphabet)

  val samplesList: java.util.List[DataPoint] = List(d1, d2, d3, d4, d5).asJava
  val samples = new RankList(samplesList)

  val trainer = new RankerTrainer
  val features = Array(DataPoint.getFeatureCount)
  for (i <- features.indices) features(i) = i + 1
  val ranker = trainer.train(RANKER_TYPE.ADARANK, List(samples).asJava, features, new PrecisionScorer())
  //ranker.save("path")
  // val ranker = new RankerFactory().loadRankerFromFile("path")
  println(ranker.toString)
}
