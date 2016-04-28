package edu.emory.mathcs.ir.liveqa.util

import dispatch.{url, _}
import Defaults._
import com.twitter.util.Future
import TwitterConverters._

/**
  * Contains utility method to download the content of a page from the web.
  */
object HtmlScraper {
  def apply(requestUrl: String): Future[Option[String]] = {
    Http(url(requestUrl) OK as.String).option
  }
}
