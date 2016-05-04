package edu.emory.mathcs.ir.liveqa

import java.util.concurrent.TimeUnit

import edu.emory.mathcs.ir.liveqa.base.{Question, MergingCandidateGenerator, Answer}
import edu.emory.mathcs.ir.liveqa.yahooanswers.{Search, YahooAnswerCandidateGenerator}
import com.twitter.util.{Await, Duration}
import com.typesafe.config.ConfigFactory
import edu.emory.mathcs.ir.liveqa.web.WebSearchCandidateGenerator

/**
  * The main question answering object, that maps a question into the answer.
  */
object QuestionAnswerer {
  // Application config
  private val cfg = ConfigFactory.load()
  private val candidateGenerator =
    new MergingCandidateGenerator(
      new YahooAnswerCandidateGenerator,
      new WebSearchCandidateGenerator)

  /**
    * Returns the answer for the given question.
    * @param question Question to answer.
    * @return Answer to the given question.
    */
  def apply(question: Question): Answer = {
    val candidates = candidateGenerator.getCandidateAnswers(question)
    val res = Await.result(candidates,
      Duration(cfg.getInt("qa.timeout"), TimeUnit.SECONDS))

    new Answer(if (res.isEmpty) "NA" else res.head.text, Array("Source"))
  }
}
