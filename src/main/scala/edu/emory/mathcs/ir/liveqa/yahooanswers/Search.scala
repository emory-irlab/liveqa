package edu.emory.mathcs.ir.liveqa.yahooanswers

import com.twitter.finagle.{Http, http}
import com.twitter.util.{Duration, Future}
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.{CandidateGeneration, Question}
import edu.emory.mathcs.ir.liveqa.util.LogFormatter
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
  val yahooAnswerSearchBaseUrl = "answers.yahoo.com:443"
  val yahooAnswerSearchUrl = "https://answers.yahoo.com/search/search_result"
  private val cfg = ConfigFactory.load()
  val client =
    Http.client
      .withTlsWithoutValidation
      .withRequestTimeout(
        Duration(cfg.getInt("request.timeout"), TimeUnit.SECONDS))
      .newService(yahooAnswerSearchBaseUrl)

  /**
    * Returns the requested number of search results from Yahoo! Answers search.
    * @param query The query to issue to Yahoo! Answers search.
    * @return A Future of search results list.
    */
  def apply(query: String, topN: Int) : Future[Seq[YahooAnswersQuestion]] = {
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
    * @param query The search query.
    * @param page The page of search results to retrieve.
    * @return A Future of search results from the given page.
    */
  private def getSearchPage(query: String, page: Int):
      Future[Seq[YahooAnswersQuestion]] = {
    val searchUrl = http.Request.queryString(yahooAnswerSearchUrl,
      Map("p" -> URLEncoder.encode(query, "UTF-8"), "s" -> page.toString))
    val request = http.RequestBuilder.create()
      .url(searchUrl)
      .buildGet()
    val results = client(request) flatMap { response =>
      Future.collect(
        if (response.status == http.Status.Ok)
          parse(response.getContentString())
            .map(qid => YahooAnswersQuestion(qid))
            .map(futureQuestion =>
              futureQuestion.filter(_.isDefined)
                            .map(questionOption => questionOption.get))
        else
          Array.empty[Future[YahooAnswersQuestion]]
      )
    } rescue {
      case exc: Exception =>
        logger.error(LogFormatter("REQUEST_EXCEPTION",
          Array(searchUrl, exc.toString)))
        Future(Seq.empty[YahooAnswersQuestion])
    }
    results
  }

  /**
    * Parses an HTML code with Yahoo! Answers search results.
    *
    * @param searchHtml Html code of Yahoo! Answers search results.
    * @return A list of results.
    */
  private def parse(searchHtml: String) : Array[String] = {
    val browser = JsoupBrowser()
    val document = browser.parseString(searchHtml)
    val answers: List[Element] =
      document >> element("#yan-questions") >> elementList("li")
    answers
      .map(answer => answer >> attr("href")("h3 a"))
      .map(href => href.split("qid=")(1))
      .toArray
  }
}
