package edu.emory.mathcs.ir.liveqa.verticals.web

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import net.ettinsmoor.{Bingerator, WebResult}

/**
  * Performs the search using Bing Web Search API and returns result documents.
  */
object WebSearch extends LazyLogging {
  private val cfg = ConfigFactory.load()
  private var currentKeyIndex = 0
  private val keys = cfg.getStringList("bing.keys")
  private var bing = restartBing()

  def apply(query: String): Seq[WebDocument] = {
    for (i <- 1 to keys.size()) {
      try {
        return bing.SearchWeb(query).take(cfg.getInt("qa.web_answers_results"))
          .map(convert(_))
      } catch {
        case exc: java.io.IOException if exc.getMessage.contains("503") =>
          logger.error(exc.getMessage)
          logger.info("Switching to another Bing API key")
          currentKeyIndex = (currentKeyIndex + 1) % keys.size()
          bing = restartBing()
      }
    }
    Nil
  }

  private def convert(result: WebResult): WebDocument = {
    new WebDocument(result.url, result.title, result.description)
  }

  /**
    * Creates a new instance of Bing web search API with the new key.
    *
    * @return
    */
  private def restartBing() = new Bingerator(keys.get(currentKeyIndex))
}
