package edu.emory.mathcs.ir.liveqa.scoring.features

import java.io.{BufferedInputStream, FileInputStream}
import java.util.zip.GZIPInputStream

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import edu.emory.mathcs.ir.liveqa.util.NlpUtils

/**
 * Created by dsavenk on 5/27/16.
 */
class NpmiDictFeatures extends FeatureCalculation with LazyLogging {
  val cfg = ConfigFactory.load()
  val npmiDict = readDictionary(cfg.getString("qa.npmi_dictionary_file"))

  /**
   * Compute a set of features for the given answer candidate.
   * @param question Current question, for which the candidate was generated.
   * @param answer Answer candidate to compute features for.
   * @return A map from feature names to the corresponding values.
   */
  override def computeFeatures(question: Question, answer: AnswerCandidate): Map[String, Float] = {
    val titleTerms = NlpUtils.getLemmas(question.titleNlp).toSet
    val bodyTerms = NlpUtils.getLemmas(question.bodyNlp).toSet
    val answerTerms = NlpUtils.getLemmas(answer.textNlp).toSet

    val npmi = titleTerms
      .flatMap(titleTerm => answerTerms.map((titleTerm, _)))
      .filter(npmiDict.contains)
      .map(npmiDict.getOrElse(_, 0.0))

    val features1 = Map[String, Float](
      "NpmiPositive" -> npmi.count(_ > 0.0),
      "NpmiNegative" -> npmi.count(_ < 0.0),
      "Npmi>0.9" -> npmi.count(_ > 0.9),
      "Npmi>0.8" -> npmi.count(_ > 0.8),
      "Npmi>0.7" -> npmi.count(_ > 0.7),
      "Npmi>0.6" -> npmi.count(_ > 0.6),
      "Npmi>0.5" -> npmi.count(_ > 0.5),
      "Npmi>0.4" -> npmi.count(_ > 0.4),
      "Npmi>0.3" -> npmi.count(_ > 0.3),
      "Npmi>0.2" -> npmi.count(_ > 0.2),
      "Npmi>0.1" -> npmi.count(_ > 0.1)
    )

    val features2 = if (npmi.nonEmpty) Map(
      "MaxNpmi" -> npmi.max.toFloat,
      "MinNpmi" -> npmi.min.toFloat,
      "AverageNpmi" -> npmi.sum.toFloat / npmi.size
    )
    else Map[String, Float]()

    features1 ++ features2
  }

  def readDictionary(file: String): Map[(String, String), Double] = {
    logger.info("Reading npmi dictionary...")
    val res = scala.io.Source.fromInputStream(
      new GZIPInputStream(
        new BufferedInputStream(
          new FileInputStream(file))))
      .getLines().map(_.split("\t")).map(f => (f(0), f(1)) -> f(2).toDouble).toMap
    logger.info("Done reading npmi dictionary...")

    res
  }
}