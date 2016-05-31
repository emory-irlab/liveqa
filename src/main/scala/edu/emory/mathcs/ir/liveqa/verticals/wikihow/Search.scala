package edu.emory.mathcs.ir.liveqa.verticals.wikihow

import java.util.concurrent.TimeUnit

import com.twitter.finagle.http
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Await, Duration, Future, TimeoutException}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.util.{HtmlScraper, LogFormatter}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._

/**
  * Searches Answers.com for related questions using its own search
  * interface.
  */
object Search extends LazyLogging {
  val resultsPageDocCount = 10
  val ehowSearchBaseUrl = "www.wikihow.com:80"
  val ehowSearchUrl = "http://www.wikihow.com/wikiHowTo"
  private val cfg = ConfigFactory.load()
  implicit val timer = DefaultTimer.twitter

  /**
    * Returns the requested number of search results from Yahoo! Answers search.
    *
    * @param query The query to issue to Yahoo! Answers search.
    * @return A Future of search results list.
    */
  def apply(query: String, topN: Int)
  : Future[Seq[Option[WikiHowQuestion]]] = {
    Future.collect(
      (1 to (topN + resultsPageDocCount - 1) / resultsPageDocCount)
        .map(page => getSearchPage(query, page))
    )
      .map(pages => pages.flatten)
      .map(pages => pages.take(topN))
  }

  /**
    * Returns search results for a particular page of search results for the
    * given query.
    *
    * @param query The search query.
    * @return A Future of search results from the given page.
    */
  private def getSearchPage(query: String, page: Int):
  Future[Seq[Option[WikiHowQuestion]]] = {
    val searchUrl = http.Request.queryString(ehowSearchUrl,
      Map("search" -> query, "start" -> (page * 10).toString))

    // Make a request with 1 second time limit.
    val answers = HtmlScraper(searchUrl)
      .within(Duration(cfg.getInt("request.timeout"), TimeUnit.SECONDS))
      .flatMap {
        content =>
          Future.collect(
            if (content.isDefined)
              parse(content.get).map(WikiHowQuestion(_))
            else
              Array.empty[Future[Option[WikiHowQuestion]]])
      } rescue {
      case exc: TimeoutException =>
        logger.error(LogFormatter("SEARCH_REQUEST_EXCEPTION",
          Array(searchUrl, exc.toString)))
        // Return empty future.
        Future.value(Nil)
    }

    answers
  }

  /**
    * Parses an HTML code with Yahoo! Answers search results.
    *
    * @param searchHtml Html code of Yahoo! Answers search results.
    * @return A list of results.
    */
  private def parse(searchHtml: String) : Array[String] = {
    val browser = JsoupBrowser()
    try {
      val document = browser.parseString(searchHtml)
      (document >> attrs("href")("a.result_link")).toSet.toArray
    } catch {
      case exc: Exception =>
        logger.error(exc.getMessage)
        Array.empty[String]
    }
  }
}
