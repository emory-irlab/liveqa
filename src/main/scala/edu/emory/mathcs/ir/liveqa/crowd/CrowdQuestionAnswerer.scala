package edu.emory.mathcs.ir.liveqa.crowd

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.TextQuestionAnswerer
import edu.emory.mathcs.ir.liveqa.base.{Answer, AnswerCandidate, CandidateGeneration, Question}
import org.joda.time.{DateTime, Seconds}

/**
  * Uses crowdsourcing to improve question answering.
  */
class CrowdQuestionAnswerer(candidateGenerator: CandidateGeneration)
  extends TextQuestionAnswerer(candidateGenerator) with LazyLogging {
  val cfg = ConfigFactory.load()
  val questionTimeout = cfg.getInt("qa.timeout")
  val safetyTimeGap = cfg.getInt("qa.crowd.time_gap")

  override def generateCandidates(question: Question): Seq[AnswerCandidate] = {
    CrowdDb.postQuestion(question)
    super.generateCandidates(question)
  }

  override def generateAnswer(question: Question,
                              rankedCandidates: Seq[AnswerCandidate]): Answer = {

    // Add candidates to the database so workers could rate them.
    CrowdDb.addAnswers(rankedCandidates.take(3).zipWithIndex.map {
      case (c, rank) => (question.qid, c.text, c.source, "", rank, c.answerType)
    })

    // Compute how many seconds do we have left.
    val secondsLeft = Seconds.secondsBetween(
      DateTime.now,
      question.submittedTime.plusSeconds(questionTimeout)
        .minusSeconds(safetyTimeGap)).getSeconds

    logger.info("Sleeping for " + secondsLeft + " seconds")
    Thread.sleep(secondsLeft * 1000)
    logger.info("Waking up and getting the answer.")

    val crowdAnswers = CrowdDb.getWorkerAnswer(question.qid)
    val answerRatings = CrowdDb.getRatedAnswers(question.qid)

    val answer = super.generateAnswer(question, rankedCandidates)
    CrowdDb.setAnswered(question)
    answer
  }
}
