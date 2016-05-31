package edu.emory.mathcs.ir.liveqa.util

import java.io.{BufferedInputStream, BufferedReader, FileInputStream, InputStreamReader}
import java.util.zip.GZIPInputStream

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

/**
  * Returns inverse document frequencies of terms.
  */
object TermIdf extends LazyLogging {
  val cfg = ConfigFactory.load()

  val termCounts = loadIdfData()
  assert(termCounts.contains(""))
  val docCount = termCounts.get("").get / 10

  private def loadIdfData(): Map[String, Long] = {
    logger.info("Reading term document count information")
    val reader = scala.io.Source.fromInputStream(
      new GZIPInputStream(
        new BufferedInputStream(
          new FileInputStream(
            cfg.getString("qa.term_doccount_file")))))

    val data = reader.getLines.map { line =>
      val fields = line.split("\t")
      if (fields.length > 1)
        (fields(0).toLowerCase, fields(1).toLong)
      else
        ("", fields(0).toLong)
    }.toList
    reader.close()
    val termCounts = data.groupBy(_._1).mapValues(l => l.map(_._2).sum)
    logger.info("Done reading term counts")

    termCounts
  }

  def apply(term: String): Float = {
    val tf = termCounts.getOrElse(term, 1L)
    math.log((docCount - tf + 0.5f) / (tf + 0.5)).toFloat
  }
}
