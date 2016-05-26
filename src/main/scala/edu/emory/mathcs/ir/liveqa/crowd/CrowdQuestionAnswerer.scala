package edu.emory.mathcs.ir.liveqa.crowd

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.TextQuestionAnswerer
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.CandidateRank
import edu.emory.mathcs.ir.liveqa.base.{Answer, AnswerCandidate, CandidateGeneration, Question}
import edu.emory.mathcs.ir.liveqa.crowd.CrowdDb.CrowdRating
import edu.emory.mathcs.ir.liveqa.ranking.AnswerRanking
import org.joda.time.{DateTime, Seconds}

/**
  * Uses crowdsourcing to improve question answering.
  */
class CrowdQuestionAnswerer(candidateGenerator: CandidateGeneration,
                            ranker: AnswerRanking)
  extends TextQuestionAnswerer(candidateGenerator, ranker) with LazyLogging {
  val cfg = ConfigFactory.load()
  val questionTimeout = cfg.getInt("qa.timeout")
  val safetyTimeGap = cfg.getInt("qa.crowd.time_gap")

  override def generateCandidates(question: Question): Seq[AnswerCandidate] = {
    CrowdDb.postQuestion(question)
    super.generateCandidates(question)
  }

  override def generateAnswer(question: Question,
                              rankedCandidates: Seq[AnswerCandidate]): Answer = {
    val ratedAnswers =
      rankedCandidates.take(cfg.getInt("qa.crowd.topn_for_rating")).toList

    // Add candidates to the database so workers could rate them.
    CrowdDb.addAnswers(ratedAnswers.zipWithIndex.map {
      case (c, rank) =>
        c.attributes(CandidateRank) = rank.toString
        (question.qid, c.text, c.source, "", rank, c.answerType)
    })

    // Compute how many seconds do we have left.
    val secondsLeft = Seconds.secondsBetween(
      DateTime.now,
      question.submittedTime.plusSeconds(questionTimeout)
        .minusSeconds(safetyTimeGap)).getSeconds

    logger.info("Sleeping for " + secondsLeft + " seconds")
    Thread.sleep(secondsLeft * 1000)
    logger.info("Waking up and getting the answer.")

    // Get answers provided by the crowd and ratings for the answers.
    val crowdAnswers = CrowdDb.getWorkerAnswer(question.qid).toList
    val answerRatings = CrowdDb.getRatedAnswers(question.qid)

    val finalCandidateList = ratedAnswers ::: crowdAnswers
    finalCandidateList.foreach { answerCandidate: AnswerCandidate =>
      val ratings = answerRatings.getOrElse(answerCandidate.text, Nil)

      // Add the attribute, based on the crowd provided score.
      answerCandidate.attributes(CrowdRating) =
        if (ratings.nonEmpty) (1.0 * ratings.sum / ratings.size).toString
        else "0.0"
    }

    val answer =
      if (finalCandidateList.isEmpty) {
        super.generateAnswer(question, rankedCandidates)
      } else {
        // Sort candidates
        val sortedByRating = finalCandidateList
          .sorted(Ordering by {
            answer: AnswerCandidate =>
              (-answer.attributes.get(CrowdRating).get.toDouble,
                answer.attributes.getOrElse(CandidateRank, "-1").toInt)
          })

        // Return the top answer if has rating greater than 2, or the longest
        // worker answer.
        if (sortedByRating.head.attributes.get(CrowdRating).get.toDouble >= 2
          || crowdAnswers.isEmpty) {
          new Answer(sortedByRating.head.text, Array(sortedByRating.head.source))
        } else {
          new Answer(crowdAnswers.sortBy(a => a.text.length).reverse.head.text,
            Array("Crowdsourcing"))
        }
    }
    CrowdDb.setAnswered(question)
    answer
  }
}
