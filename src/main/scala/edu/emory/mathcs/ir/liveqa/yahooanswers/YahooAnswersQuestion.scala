package edu.emory.mathcs.ir.liveqa.yahooanswers

import java.net.URLEncoder
import javax.swing.text.html.HTML

import com.twitter.finagle.{Http, http}
import com.twitter.util.Future
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Element

/**
  * A question and answers from Yahoo! Answers.
  *
  * @param qid Unique question identifier
  * @param categories The categories of the question
  * @param title The title of the question
  * @param body The body of the question
  * @param answers The list of answers
  */
case class YahooAnswersQuestion(qid: String, categories: Array[String],
                                title: String, body: String,
                                answers: Array[String])

/**
  * Companion object for YahooAnswersQuestion class, it has a factory method to
  * get the question data from Yahoo! Answers given its qid.
  */
object YahooAnswersQuestion {
  val yahooAnswerSearchBaseUrl = "answers.yahoo.com:443"
  val yahooAnswerSearchUrl = "https://answers.yahoo.com/question/index"
  val client =
    Http.client.withTlsWithoutValidation.newClient(yahooAnswerSearchBaseUrl)
      .toService

  def apply(qid: String) : Future[Option[YahooAnswersQuestion]] = {
    val request = http.RequestBuilder.create()
      .url(http.Request.queryString(yahooAnswerSearchUrl, Map("qid" -> qid)))
      .buildGet()
    client(request).map { response =>
      if (response.status == http.Status.Ok)
        Some(parse(response.getContentString()))
      else
        None
    }
  }

  private def parse(pageHtml: String): YahooAnswersQuestion = {
    val browser = JsoupBrowser()
    val document = browser.parseString(pageHtml)
    val qid = document >> attr("data-ya-question-id")("#ya-question-detail")
    val categoriesElements = document >> elementList("#ya-question-breadcrumb a")
    val categories = categoriesElements.flatMap {
      element => if (element.attrs.contains("title"))
        Some(element.attr("title"))
      else None
    }
    val title = document >> text("h1")
    val body = document >> text(".ya-q-text")
    val answers = document >> texts(".ya-q-full-text")
    new YahooAnswersQuestion(qid, categories.toArray, title, body, answers.toArray)
  }
}