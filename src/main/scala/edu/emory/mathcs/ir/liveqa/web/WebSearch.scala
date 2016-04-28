package edu.emory.mathcs.ir.liveqa.web

import com.typesafe.config.ConfigFactory
import net.ettinsmoor.{Bingerator, WebResult}

/**
  * Performs the search using Bing Web Search API and returns result documents.
  */
object WebSearch {
  private val cfg = ConfigFactory.load()
  private var currentKeyIndex = 0
  private val keys = cfg.getStringList("bing.keys")
  private var bing = new Bingerator(keys.get(currentKeyIndex))

  def apply(query: String): Seq[WebDocument] = {
    // TODO(denxx): What happens if it runs out of quota?
    bing.SearchWeb(query).take(cfg.getInt("qa.web_answers_results"))
      .map(convert(_))
  }

  private def convert(result: WebResult): WebDocument = {
    new WebDocument(result.url, result.title, result.description)
  }
}
