package edu.emory.mathcs.ir.liveqa.crowd

import edu.emory.mathcs.ir.liveqa.TextQuestionAnswerer
import edu.emory.mathcs.ir.liveqa.base.{Answer, AnswerCandidate, CandidateGeneration, Question}

/**
  * Uses crowdsourcing to improve question answering.
  */
class CrowdQuestionAnswerer(candidateGenerator: CandidateGeneration)
  extends TextQuestionAnswerer(candidateGenerator) {

  override def generateCandidates(question: Question): Seq[AnswerCandidate] = {
    CrowdDb.postQuestion(question)
    super.generateCandidates(question)
  }

  override def generateAnswer(question: Question,
                              rankedCandidates: Seq[AnswerCandidate]): Answer = {
    val answer = super.generateAnswer(question, rankedCandidates)
    CrowdDb.setAnswered(question)
    answer
  }
}
