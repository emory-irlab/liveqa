package edu.emory.mathcs.ir.liveqa.verticals.answerscom

import com.twitter.util.Future
import com.typesafe.config.ConfigFactory
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.ANSWERS_COM
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, CandidateGeneration, QueryGeneration, Question}

/**
  * An object that generates candidates answers for the given question by
  * retrieving related questions using Yahoo! Answers search functionality.
  */
class AnswersComCandidateGenerator
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
        Search(_, cfg.getInt("qa.answers_com_results"))
      } map {
        futureResults => futureResults.map(
          results => results.flatten.flatMap(createCandidates(_)))
      }
    }
    results.map(futureResults => futureResults.flatten)
  }

  private def createCandidates(q: AnswersComQuestion): Seq[AnswerCandidate] = {
    val candidate = new AnswerCandidate(ANSWERS_COM, q.answer, q.url)

    // Add question attributes, so we could use them later.
    candidate.attributes(AnswerCandidate.CandidateSourceRank) = "0"
    candidate.attributes(AnswerCandidate.QuestionTitle) = q.title
    candidate.attributes(AnswerCandidate.QuestionBody) = q.body
    candidate.attributes(AnswerCandidate.QuestionMainCategory) = q.categories.headOption.getOrElse("")
    candidate.attributes(AnswerCandidate.QuestionCategories) = q.categories.mkString("\t")

    // Return the answer.
    Seq(candidate)
  }
}