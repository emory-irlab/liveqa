package edu.emory.mathcs.ir.liveqa.yahooanswers

import com.twitter.util.Future
import com.typesafe.config.ConfigFactory
import edu.emory.mathcs.ir.liveqa.{AnswerCandidate, CandidateGeneration, Question}

/**
  * Created by dsavenk on 4/21/16.
  */
object YahooAnswerCandidateGenerator extends CandidateGeneration {
  val cfg = ConfigFactory.load()

  def getSearchQueries(question: Question) = {
    Seq(question.title)
  }

  override def getCandidateAnswers(question: Question): Future[Seq[AnswerCandidate]] = {
    val results = Future collect {
      getSearchQueries(question) map {
        Search(_, cfg.getInt("qa.yahoo_answers_results"))
      } map {
        futureResults => futureResults map {
          results => results.map(res =>
            // TODO(denxx): Add additional information to this instance.
            new AnswerCandidate(AnswerCandidate.YAHOO_ANSWERS,
              res.answers.head, res.qid))
        }
      }
    } map { futureResults => futureResults.flatten }
    results
  }
}
