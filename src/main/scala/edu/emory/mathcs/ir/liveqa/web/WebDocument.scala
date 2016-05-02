package edu.emory.mathcs.ir.liveqa.web

import java.util.concurrent.TimeUnit

import com.twitter.finagle.{Http, http}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Duration, Future}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.util.{HtmlScraper, LogFormatter}

/**
  * Represents a web document.
  */
class WebDocument(val url: String, val title: String, val description: String) {
  /**
    * The content of the document.
    */
  lazy val content: Future[Option[String]] = WebDocument.scrapeContent(url)

  /**
    * Simple constructor, that creates an instance using url only. The other
    * fields are set to empty.
    *
    * @param url The url of the document.
    * @return An instance of [[WebDocument]] class
    */
  def this(url: String) = this(url, "", "")
}


object WebDocument extends LazyLogging {
  implicit val timer = DefaultTimer.twitter
  private val cfg = ConfigFactory.load()

  /**
    * Returns the future which can contain the content of the webpage with the
    * given url if everything goes ok. Otherwise, it returns None.
    * @param url Url of a webpage to download.
    * @return The [[Future]] with an [[Option]], which might contain the content
    *         of the web page.
    */
  def scrapeContent(url: String): Future[Option[String]] = {
    val document = HtmlScraper(url)
      .within(Duration(cfg.getInt("request.timeout"), TimeUnit.SECONDS))
      .rescue {
        case exc: com.twitter.util.TimeoutException =>
          logger.error(LogFormatter("WEB_REQUEST_TIMEOUT",
            Array(url, exc.toString)))
          // Return empty future.
          Future.value(None)
      }
    document
  }
}