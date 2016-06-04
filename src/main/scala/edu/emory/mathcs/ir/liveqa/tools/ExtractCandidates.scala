package edu.emory.mathcs.ir.liveqa.tools

import java.io.{PrintWriter, File}

import edu.emory.mathcs.ir.liveqa.verticals.web.ContentExtractor
import edu.emory.mathcs.ir.liveqa.verticals.yahooanswers.YahooAnswersQuestion
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

/**
 * Created by dsavenk on 5/29/16.
 */
object ExtractCandidates extends App {
  val browser = new JsoupBrowser

  scala.io.Source.fromFile(args(0) + "urls.txt").getLines().zipWithIndex.foreach { case (url, index) =>
    try {
      val content = scala.io.Source.fromFile(args(0) + (index + 5) + ".txt").mkString
      if (index % 100 == 0) println("Processed " + index + " documents")
      url match {
        case u: String if u.contains("answers.yahoo.com") =>
          val question = YahooAnswersQuestion.parse(content)
          val out = new PrintWriter(new File(args(0) + (index + 5) + "_content.txt"))
          out.println(question.qid)
          out.println(question.categories.mkString("\t"))
          out.println(question.title.replace("\n", " "))
          out.println(question.body.replace("\n", " "))
          out.close()
        case _ =>
          val out = new PrintWriter(new File(args(0) + (index + 5) + "_content.txt"))
          out.println(browser.parseString(content).title.replace("\n", " "))
          ContentExtractor.apply(content).foreach {
            block => out.println(block.replace("\n", " "))
          }
          out.close()
      }
    } catch {
      case exc: Exception =>
        System.err.println(exc.getMessage)
    }
  }
}
