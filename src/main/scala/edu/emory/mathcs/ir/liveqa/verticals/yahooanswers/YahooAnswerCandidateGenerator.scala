package edu.emory.mathcs.ir.liveqa.verticals.yahooanswers

import com.twitter.util.Future
import com.typesafe.config.ConfigFactory
import edu.emory.mathcs.ir.liveqa.base.{Question, QueryGeneration, CandidateGeneration, AnswerCandidate}
import AnswerCandidate.YAHOO_ANSWERS

/**
  * An object that generates candidates answers for the given question by
  * retrieving related questions using Yahoo! Answers search functionality.
  */
class YahooAnswerCandidateGenerator
      extends CandidateGeneration with QueryGeneration {
  private val cfg = ConfigFactory.load()

  /**
    * Generates search queries for the given question.
    * @param question A question to generate search queries for.
    * @return A sequence of search queries to issue to a search engine.
    */
  override def getSearchQueries(question: Question) = {
    Seq(question.title.replaceAll("[^A-Za-z0-9]", " "))
  }

  override def getCandidateAnswers(question: Question)
      : Future[Seq[AnswerCandidate]] = {
    val results = Future collect {
      getSearchQueries(question) map {
        Search(_, cfg.getInt("qa.yahoo_answers_results"))
      } map {
        futureResults => futureResults.map(
          results => results.flatten.filter(_.qid != question.qid)
            .flatMap(createCandidates(_)))
      }
    }
    results.map(futureResults => futureResults.flatten)
  }

  private def createCandidates(questionAnswers: YahooAnswersQuestion)
      :Seq[AnswerCandidate] = {
    questionAnswers.answers
      .zipWithIndex
      .map {
        case (answer: String, rank: Int) =>
          val candidate = new AnswerCandidate(
            YAHOO_ANSWERS, answer, questionAnswers.url)

          // Add question attributes, so we could use them later.
          candidate.attributes(AnswerCandidate.CandidateSourceRank) = rank.toString
          candidate.attributes(AnswerCandidate.QuestionTitle) =
            questionAnswers.title
          candidate.attributes(AnswerCandidate.QuestionBody) =
            questionAnswers.body
          candidate.attributes(AnswerCandidate.QuestionMainCategory) =
            questionAnswers.categories.headOption.getOrElse("")
          candidate.attributes(AnswerCandidate.QuestionCategories) =
            questionAnswers.categories.mkString("\t")
          candidate.attributes(AnswerCandidate.QuestionCategories) =
            questionAnswers.qid

          // Return the answer.
          candidate
      }
  }
}
