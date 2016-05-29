package edu.emory.mathcs.ir.liveqa.util

import dispatch.{url, _}
import Defaults._
import com.twitter.util.Future
import TwitterConverters._
import com.typesafe.scalalogging.LazyLogging

/**
  * Contains utility method to download the content of a page from the web.
  */
object HtmlScraper extends LazyLogging {
  val http = Http.configure(_ setFollowRedirect true)
  def apply(requestUrl: String): Future[Option[String]] = {
    val futureResponse = http(url(requestUrl) OK as.String)
    futureResponse onFailure {
      case exc: Exception =>
        logger.error(exc.getMessage)
    }
    futureResponse.option
  }

  def shutdown(): Unit = {
    http.shutdown()
    Http.shutdown()
  }
}
