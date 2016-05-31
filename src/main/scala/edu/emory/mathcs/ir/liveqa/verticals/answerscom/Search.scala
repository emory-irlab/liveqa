package edu.emory.mathcs.ir.liveqa.verticals.answerscom

import com.twitter.finagle.{Http, http}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Await, Duration, Future, TimeoutException}
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
  * Searches Answers.com for related questions using its own search
  * interface.
  */
object Search extends LazyLogging {
  val resultsPageDocCount = 10
  val answersComSearchBaseUrl = "www.answers.com:80"
  val answersComSearchUrl = "http://www.answers.com/Q/"
  private val cfg = ConfigFactory.load()
  implicit val timer = DefaultTimer.twitter

  /**
    * Returns the requested number of search results from Yahoo! Answers search.
    *
    * @param query The query to issue to Yahoo! Answers search.
    * @return A Future of search results list.
    */
  def apply(query: String, topN: Int)
  : Future[Seq[Option[AnswersComQuestion]]] = {
      Future.collect(Seq(getSearchPage(query)))
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
  private def getSearchPage(query: String):
  Future[Seq[Option[AnswersComQuestion]]] = {
    val searchUrl = answersComSearchUrl + URLEncoder.encode(query, "UTF-8")

    // Make a request with 1 second time limit.
    val answers = HtmlScraper(searchUrl)
      .within(Duration(cfg.getInt("request.timeout"), TimeUnit.SECONDS))
      .flatMap {
        content =>
          Future.collect(
            if (content.isDefined)
              parse(content.get).map(AnswersComQuestion(_))
            else
              Array.empty[Future[Option[AnswersComQuestion]]])
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
      val articles = document >> elementList("article.frame")
      articles
        .map(answer => answer >> attr("href")("h1 a"))
        .filter(!_.contains("/article/")).toSet
        .toArray
    } catch {
      case exc: Exception =>
        logger.error(exc.toString)
        Array.empty[String]
    }
  }
}
