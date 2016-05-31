package edu.emory.mathcs.ir.liveqa.util

import collection.JavaConverters._
import edu.stanford.nlp.simple.Document

/**
 * Created by dsavenk on 5/27/16.
 */
object NlpUtils {
  def getLemmas(doc: Document): Seq[String] = {
    doc.sentences().asScala
      .flatMap(s => s.lemmas.asScala)
      .map(_.toLowerCase)
      .filter(_.headOption.getOrElse(' ').isLetterOrDigit)
  }
}
