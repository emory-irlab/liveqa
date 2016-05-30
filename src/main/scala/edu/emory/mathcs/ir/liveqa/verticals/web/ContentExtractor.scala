package edu.emory.mathcs.ir.liveqa.verticals.web

import collection.JavaConverters._
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.html.BoilerpipeContentHandler
import org.apache.tika.parser.{AutoDetectParser, ParseContext}
import org.apache.tika.sax.BodyContentHandler

/**
  * Created by dsavenk on 4/28/16.
  */
object ContentExtractor extends LazyLogging {
  val cfg = ConfigFactory.load()
  val contentMinLength = cfg.getInt("qa.web_minimum_candidate_length")
  val maxDocumentLength = cfg.getInt("qa.max_document_length_to_parse")

  def apply(htmlCode: String): Seq[String] = {
    val textHandler = new BoilerpipeContentHandler(new BodyContentHandler(maxDocumentLength))
    val metadata = new Metadata()
    val parser = new AutoDetectParser()
    val context = new ParseContext()
    try {
      parser.parse(new ByteArrayInputStream(
        htmlCode.getBytes(StandardCharsets.UTF_8)),
        textHandler,
        metadata,
        context)
      val content = textHandler.getTextDocument.getTextBlocks.asScala
        .filter(_.isContent)
        .map(_.getText)
        .filter(_.length >= contentMinLength)

      content
    } catch {
      case exc: Exception => logger.error(exc.getMessage)
        Nil
    }
  }
}
