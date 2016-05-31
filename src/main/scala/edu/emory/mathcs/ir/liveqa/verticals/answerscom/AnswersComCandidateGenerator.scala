package edu.emory.mathcs.ir.liveqa.verticals.answerscom

import com.twitter.util.Future
import com.typesafe.config.ConfigFactory
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.ANSWERS_COM
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, CandidateGeneration, QueryGeneration, Question}

/**
  * An object that generates candidates answers for the given question by
  * retrieving related questions using Yahoo! Answers search functionality.
  */
class AnswersComCandidateGenerator(queryGenerator: QueryGeneration)
  extends CandidateGeneration {
  private val cfg = ConfigFactory.load()

  override def getCandidateAnswers(question: Question)
      : Future[Seq[AnswerCandidate]] = {
    val results = Future collect {
      queryGenerator.getSearchQueries(question) map {
        Search(_, cfg.getInt("qa.answers_com_results"))
      } map {
        futureResults => futureResults.map(
          results => results.flatten.flatMap(createCandidates(_)))
      }
    }
    results.map(futureResults => futureResults.flatten)
      .map(candidates => candidates.map(c => c.text -> c).toMap.values.toSeq)
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