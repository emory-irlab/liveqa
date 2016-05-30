package edu.emory.mathcs.ir.liveqa.verticals.wikihow

import com.twitter.util.Future
import com.typesafe.config.ConfigFactory
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.WIKIHOW
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, CandidateGeneration, Question}

/**
  * Created by dsavenk on 5/28/16.
  */
class WikiHowCandidateGenerator extends CandidateGeneration {

  private val cfg = ConfigFactory.load()

  /**
    * Generates search queries for the given question.
    * @param question A question to generate search queries for.
    * @return A sequence of search queries to issue to a search engine.
    */
  def getSearchQueries(question: Question) = {
    Seq(question.title.replaceAll("[^A-Za-z0-9]", " "))
  }

  override def getCandidateAnswers(question: Question)
  : Future[Seq[AnswerCandidate]] = {
    val results = Future collect {
      getSearchQueries(question) map {
        Search(_, cfg.getInt("qa.wikihow_results"))
      } map {
        futureResults => futureResults.map(
          results => results.flatten.flatMap(createCandidates(_)))
      }
    }
    results.map(futureResults => futureResults.flatten)
  }

  private def createCandidates(q: WikiHowQuestion): Seq[AnswerCandidate] = {
    val candidate = new AnswerCandidate(WIKIHOW, q.answer, q.url)

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