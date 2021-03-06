package edu.emory.mathcs.ir.liveqa.verticals.yahooanswers

import com.twitter.finagle.{Http, http}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Duration, Future, TimeoutException}
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.base.Question
import edu.emory.mathcs.ir.liveqa.util.{HtmlScraper, LogFormatter}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.model.Element

/**
  * Searches Yahoo! Answers for related questions using its own search
  * interface.
  */
object Search extends LazyLogging {
  val resultsPageDocCount = 10
  //val yahooAnswerSearchBaseUrl = "answers.yahoo.com:443"
  val yahooAnswerSearchBaseUrl = "answers.search.yahoo.com:443"
  //val yahooAnswerSearchUrl = "https://answers.yahoo.com/search/search_result"
  val yahooAnswerSearchUrl = "https://answers.search.yahoo.com/search"
  private val cfg = ConfigFactory.load()
  implicit val timer = DefaultTimer.twitter

  /**
    * Returns the requested number of search results from Yahoo! Answers search.
    *
    * @param query The query to issue to Yahoo! Answers search.
    * @return A Future of search results list.
    */
  def apply(query: String, topN: Int)
      : Future[Seq[Option[YahooAnswersQuestion]]] = {
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
    * @param page The page of search results to retrieve.
    * @return A Future of search results from the given page.
    */
  private def getSearchPage(query: String, page: Int):
      Future[Seq[Option[YahooAnswersQuestion]]] = {
    val searchUrl = http.Request.queryString(yahooAnswerSearchUrl,
      Map("p" -> query, "s" -> page.toString))

    // For some reason Yahoo! Answers doesn't find anything if plus is used
    val answers = HtmlScraper(searchUrl)
      .within(Duration(cfg.getInt("request.timeout"), TimeUnit.SECONDS))
      .flatMap {
        content =>
          val res = Future.collect(
            if (content.isDefined)
              parse(content.get).map(YahooAnswersQuestion(_))
            else
              Array.empty[Future[Option[YahooAnswersQuestion]]])
          res rescue {
            case exc: TimeoutException =>
              logger.error(LogFormatter("SEARCH_REQUEST_EXCEPTION",
                Array(searchUrl, exc.toString)))
              // Return empty future.
              Future.value(Nil)
          }
          res
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
      //document >> element("#yan-questions") >> elementList("li")
      (document >> attrs("href")(".searchCenterMiddle li a"))
        .filter(_.contains("qid="))
        .map(href => href.split("qid=")(1).split("&")(0)).toSet
        .toArray
    } catch {
      case exc: Exception =>
        logger.error(exc.getMessage)
        Array.empty[String]
    }
  }
}
