package edu.emory.mathcs.ir.liveqa

import java.util.concurrent.TimeUnit

import edu.emory.mathcs.ir.liveqa.yahooanswers.Search
import com.twitter.util.{Await, Duration}
import com.typesafe.config.ConfigFactory

/**
  * The main question answering object, that maps a question into the answer.
  */
object QuestionAnswerer {
  // Application config
  private val cfg = ConfigFactory.load()

  /**
    * Returns the answer for the given question.
    * @param question Question to answer.
    * @return Answer to the given question.
    */
  def apply(question: Question): Answer = {
    val yahooAnswersResults = Search(question.title,
      cfg.getInt("qa.yahoo_answers_results"))
    val res = Await.result(yahooAnswersResults,
      Duration(cfg.getInt("qa.timeout"), TimeUnit.SECONDS))

    val answers = res.map(qna => qna.qid).mkString(" -- ")
    new Answer(answers, Array("Source"))
  }
}
