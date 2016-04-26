package edu.emory.mathcs.ir.liveqa.yahooanswers

import java.util.concurrent.TimeUnit

import com.twitter.finagle.{Http, http}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Duration, Future}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.util.LogFormatter
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._

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
                                answers: Array[String]) {
  /**
    * @return Returns url of the current YahooAnswersQuestions.
    */
  def url: String = YahooAnswersQuestion.url(qid)
}

/**
  * Companion object for YahooAnswersQuestion class, it has a factory method to
  * get the question data from Yahoo! Answers given its qid.
  */
object YahooAnswersQuestion extends LazyLogging {
  val YahooAnswerSearchBaseUrl = "answers.yahoo.com:443"
  val YahooAnswerSearchUrl = "https://answers.yahoo.com/question/index"

  implicit val timer = DefaultTimer.twitter

  private val cfg = ConfigFactory.load()
  val client = Http.client
    .withTlsWithoutValidation
    .newService(YahooAnswerSearchBaseUrl)

  /**
    * Scrapes the data for the Yahoo! Answers question with the provided qid.
 *
    * @param qid QID of the question to scrape from Yahoo! Answers.
    * @return A future, that might contain [[YahooAnswersQuestion]] object with
    *         the data for the provided Qid. If request fails it will be None.
    */
  def apply(qid: String) : Future[Option[YahooAnswersQuestion]] = {
    val requestUrl = url(qid)
    val request = http.RequestBuilder.create()
      .url(requestUrl)
      .buildGet()

    // Request the page with the question from Yahoo! Answers and parse it.
    val qna = client(request)
      .within(Duration(cfg.getInt("request.timeout"), TimeUnit.SECONDS))
      .map {
        response => if (response.status == http.Status.Ok)
          Some(parse(response.getContentString()))
        else
          None
      } rescue {
        case exc: com.twitter.util.TimeoutException =>
          logger.error(LogFormatter("REQUEST_TIMEOUT",
            Array(requestUrl, exc.toString)))
          // Return empty future.
          Future.value(None)
      }

    qna
  }

  /**
    * Returns a url of the question with the given QID.
 *
    * @param qid Unique question identifier on Yahoo! Answers.
    * @return Url of the question on Yahoo! Answers.
    */
  def url(qid: String): String = {
    http.Request.queryString(
      YahooAnswerSearchUrl, Map("qid" -> qid))
  }

  /**
    * Parses the HTML code of the Yahoo! Answers question web page and returns
    * [[YahooAnswersQuestion]] object with the corresponding data.
 *
    * @param pageHtml Html code of the question page.
    * @return [[YahooAnswersQuestion]] instance for the given question.
    */
  private def parse(pageHtml: String): YahooAnswersQuestion = {
    val browser = JsoupBrowser()
    val document = browser.parseString(pageHtml)
    val qid = document >> attr("data-ya-question-id")("#ya-question-detail")
    val categoriesElements =
      document >> elementList("#ya-question-breadcrumb a")
    val categories = categoriesElements.flatMap {
      element =>
        if (element.attrs.contains("title"))
          Some(element.attr("title"))
        else
          None
    }
    val title = document >> text("h1")
    val body = document >> text(".ya-q-text")
    val answers = document >> texts(".ya-q-full-text")

    // TODO(denxx): Extract additional answer metainformation, e.g. votes.
    new YahooAnswersQuestion(qid, categories.toArray, title, body,
      answers.toArray)
  }
}