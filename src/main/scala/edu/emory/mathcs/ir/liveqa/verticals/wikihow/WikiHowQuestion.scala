package edu.emory.mathcs.ir.liveqa.verticals.wikihow

import java.util.concurrent.TimeUnit

import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Duration, Future}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.util.{HtmlScraper, LogFormatter}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

/**
  * A question and answers from Yahoo! Answers.
  *
  * @param url Url of the question
  * @param categories The categories of the question
  * @param title The title of the question
  * @param body The body of the question
  * @param answer The list of answers
  */
case class WikiHowQuestion(url: String, categories: Array[String],
                           title: String, body: String,
                           answer: String)

/**
  * Companion object for YahooAnswersQuestion class, it has a factory method to
  * get the question data from Yahoo! Answers given its qid.
  */
object WikiHowQuestion extends LazyLogging {
  implicit val timer = DefaultTimer.twitter
  private val cfg = ConfigFactory.load()

  /**
    * Scrapes the data for the Yahoo! Answers question with the provided qid.
    *
    * @param url URL of question.
    * @return A future, that might contain [[WikiHowQuestion]] object with
    *         the data for the provided Qid. If request fails it will be None.
    */
  def apply(url: String) : Future[Option[WikiHowQuestion]] = {
    // Request the page with the question from Yahoo! Answers and parse it.
    val qna = HtmlScraper(if (!url.startsWith("http")) "http:" + url else url)
      .within(Duration(cfg.getInt("request.timeout"), TimeUnit.SECONDS))
      .map(contentOption => contentOption.map(parse(url, _)))
      .rescue {
        case exc: com.twitter.util.TimeoutException =>
          logger.error(LogFormatter("REQUEST_TIMEOUT",
            Array(url, exc.toString)))
          // Return empty future.
          Future.value(None)
        case exc: Exception =>
          logger.error(exc.toString)
          Future.value(None)
      }

    qna
  }

  /**
    * Parses the HTML code of the Yahoo! Answers question web page and returns
    * [[WikiHowQuestion]] object with the corresponding data.
    *
    * @param pageHtml Html code of the question page.
    * @return [[WikiHowQuestion]] instance for the given question.
    */
  private def parse(url: String, pageHtml: String): WikiHowQuestion = {
    val browser = JsoupBrowser()
    val document = browser.parseString(pageHtml)
    val categories = document >> texts("ul#breadcrumb li")
    val title = document >> text("h1")
    val answer = (document >> texts("#intro p")).mkString("\n")

    new WikiHowQuestion(url, categories.toArray, title, "", answer)
  }
}