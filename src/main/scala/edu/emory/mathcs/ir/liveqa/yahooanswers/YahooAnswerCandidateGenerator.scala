package edu.emory.mathcs.ir.liveqa.yahooanswers

import com.twitter.util.Future
import com.typesafe.config.ConfigFactory
import edu.emory.mathcs.ir.liveqa.AnswerCandidate.YAHOO_ANSWERS
import edu.emory.mathcs.ir.liveqa.{AnswerCandidate, CandidateGeneration, Question}

/**
  * An object that generates candidates answers for the given question by
  * retrieving related questions using Yahoo! Answers search functionality.
  */
object YahooAnswerCandidateGenerator extends CandidateGeneration {
  private val cfg = ConfigFactory.load()

  def getSearchQueries(question: Question) = {
    Seq(question.title)
  }

  override def getCandidateAnswers(question: Question): Future[Seq[AnswerCandidate]] = {
    val results = Future collect {
      getSearchQueries(question) map {
        Search(_, cfg.getInt("qa.yahoo_answers_results"))
      } map {
        futureResults => futureResults map {
          results => results.flatten.flatMap(createCandidates(_))
        }
      }
    } map { futureResults => futureResults.flatten }
    results
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
          candidate.attributes(AnswerCandidate.AnswerRank) = rank.toString
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
