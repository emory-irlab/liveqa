package edu.emory.mathcs.ir.liveqa.web

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.html.BoilerpipeContentHandler
import org.apache.tika.parser.{AutoDetectParser, ParseContext}
import org.apache.tika.sax.BodyContentHandler

/**
  * Created by dsavenk on 4/28/16.
  */
object ContentExtractor {

  def apply(htmlCode: String) = {
    val textHandler = new BoilerpipeContentHandler(new BodyContentHandler())
    val metadata = new Metadata()
    val parser = new AutoDetectParser()
    val context = new ParseContext()
    parser.parse(new ByteArrayInputStream(
      htmlCode.getBytes(StandardCharsets.UTF_8)),
      textHandler,
      metadata,
      context)
    for (block <- textHandler.getTextDocument.getTextBlocks.toArray) {
      println(block.toString)
    }
    textHandler.getTextDocument.getContent
  }
}
