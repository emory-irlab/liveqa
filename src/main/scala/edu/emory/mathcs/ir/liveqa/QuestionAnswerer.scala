package edu.emory.mathcs.ir.liveqa

import java.util.concurrent.TimeUnit

import edu.emory.mathcs.ir.liveqa.base._
import edu.emory.mathcs.ir.liveqa.yahooanswers.{Search, YahooAnswerCandidateGenerator}
import com.twitter.util.{Await, Duration}
import com.typesafe.config.ConfigFactory
import edu.emory.mathcs.ir.liveqa.web.WebSearchCandidateGenerator

/**
  * Main trait for question-answering modules.
  */
trait QuestionAnswering {
  /**
    * Answer the given question.
    * @param question The question to answer.
    * @return The answer to the given question.
    */
  def answer(question: Question): Answer
}

/**
  * Ranking-based question answering. There are 3 main stages, first a set of
  * candidate answers is generated ([[RankingBasedQuestionAnswering.generateCandidates]]),
  * then they are ranked ([[RankingBasedQuestionAnswering.rankCandidates]]), and
  * finally the response is generated ([[RankingBasedQuestionAnswering.generateAnswer]]).
  */
trait RankingBasedQuestionAnswering extends QuestionAnswering {

  /**
    * Generates candidate answers to the given question.
    * @param question The answer to generate candidate for.
    * @return A set of candidate answers.
    */
  def generateCandidates(question: Question): Seq[AnswerCandidate]

  /**
    * Ranks candidates answers.
     @param question The current question.
    * @param candidates A source list of answer candidates.
    * @return A ranked list of candidate answers, where the head is the best
    *         candidate.
    */
  def rankCandidates(question: Question,
                     candidates: Seq[AnswerCandidate]): Seq[AnswerCandidate]

  /**
    * Generate the final answer for the given question using the specified
    * ranked list of candidate answers.
    * @param question The current question.
    * @param rankedCandidates The ranked list of candidate answers.
    * @return The final response of the question answering system.
    */
  def generateAnswer(question: Question, rankedCandidates: Seq[AnswerCandidate]) : Answer

  /**
    * Answers the question by first generating a set of candidates, then ranking
    * them and then generating the final answer.
    * @param question The question to answer.
    * @return The answer to the given question.
    */
  override def answer(question: Question): Answer = {
    val candidates = generateCandidates(question)
    val rankedCandidates = rankCandidates(question, candidates)
    generateAnswer(question, rankedCandidates)
  }
}

/**
  * The main question answering object, that maps a question into the answer.
  */
class TextQuestionAnswerer(candidateGenerator: CandidateGeneration)
  extends RankingBasedQuestionAnswering {

  // Application config
  private val cfg = ConfigFactory.load()

  /**
    * Generates candidate answers to the given question.
    *
    * @param question The answer to generate candidate for.
    * @return A set of candidate answers.
    */
  override def generateCandidates(question: Question): Seq[AnswerCandidate] = {
    val candidates = candidateGenerator.getCandidateAnswers(question)
    Await.result(candidates, Duration(cfg.getInt("qa.timeout"), TimeUnit.SECONDS))
  }

  /**
    * Ranks candidates answers.
    *
    * @param question   The current question.
    * @param candidates A source list of answer candidates.
    * @return A ranked list of candidate answers, where the head is the best
    *         candidate.
    */
  override def rankCandidates(question: Question, candidates: Seq[AnswerCandidate]): Seq[AnswerCandidate] = {
    candidates
  }

  /**
    * Generate the final answer for the given question using the specified
    * ranked list of candidate answers.
    *
    * @param question         The current question.
    * @param rankedCandidates The ranked list of candidate answers.
    * @return The final response of the question answering system.
    */
  override def generateAnswer(question: Question, rankedCandidates: Seq[AnswerCandidate]): Answer = {
    new Answer(if (rankedCandidates.isEmpty) "NA" else rankedCandidates.head.text, Array("Source"))
  }
}
