package edu.emory.mathcs.ir.liveqa.yahooanswers

import com.twitter.finagle.{Http, http}
import com.twitter.util.Future
import java.net.URLEncoder

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Element

/**
  * Searches Yahoo! Answers for related questions using its own search
  * interface.
  */
object Search {
  val yahooAnswerSearchBaseUrl = "answers.yahoo.com:443"
  val yahooAnswerSearchUrl = "https://answers.yahoo.com/search/search_result"
  val client =
    Http.client.withTlsWithoutValidation.newClient(yahooAnswerSearchBaseUrl)
      .toService

  def apply(query: String) : Future[Seq[YahooAnswersQuestion]] = {
    val searchUrl = http.Request.queryString(yahooAnswerSearchUrl,
      Map("p" -> URLEncoder.encode(query, "UTF-8")))
    val request = http.RequestBuilder.create()
      .url(searchUrl)
      .buildGet()
    val results = client(request).flatMap { response =>
      Future.collect(
        if (response.status == http.Status.Ok)
          parse(response.getContentString())
            .map(qid => YahooAnswersQuestion(qid))
            .map(futureQuestion => futureQuestion.filter(_.isDefined)
              .map(questionOption => questionOption.get))
        else
          Array.empty[Future[YahooAnswersQuestion]]
      )
    }
    results
  }

  /**
    * Parses an HTML code with Yahoo! Answers search results.
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
