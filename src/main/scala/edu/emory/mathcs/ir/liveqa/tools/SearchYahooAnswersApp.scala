package edu.emory.mathcs.ir.liveqa.tools

import com.twitter.util.{Await, Future}
import dispatch.Http
import edu.emory.mathcs.ir.liveqa.util.HtmlScraper
import edu.emory.mathcs.ir.liveqa.verticals.yahooanswers.Search

import scala.io.Source

/**
  * Created by dsavenk on 2/28/17.
  */
object SearchYahooAnswersApp {

  def main(args: Array[String]): Unit = {
    val questions = Source.fromFile(args(0)).getLines()
    for (question <- questions) {
      val res = Future.collect(question.map("\"" + _ + "\"").map(question => Search(question, 1).map(_.headOption)))
      val searchResults = Await.result(res)
      val toPrint = searchResults.filter(_.isDefined).map(_.get).filter(_.isDefined).map(_.get)
        .map(qna => List(qna.qid, qna.title.replaceAll("\\s+", " "),
          qna.body.replaceAll("\\s+", " "), qna.categories.mkString("|").replaceAll("\\s+", " "),
          qna.answers.map(_.replaceAll("\\s+", " ")).mkString("\t")).mkString("\t"))
      toPrint.foreach(println(_))
      Thread.sleep(1000)
    }

    HtmlScraper.shutdown()
  }

}
