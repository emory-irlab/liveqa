package edu.emory.mathcs.ir.liveqa.base

import com.typesafe.config.ConfigFactory

trait CandidateAnswerTextProcessing {
  def apply(text: String): String
}

object CandidateAnswerTextProcessor {
  val cfg = ConfigFactory.load()

  implicit object AnswerTextMaxlenCutter extends CandidateAnswerTextProcessing {
    val answerMaxLength = cfg.getInt("qa.maxlen")
    override def apply(text: String): String = {
      if (text.length > answerMaxLength)
        text.substring(0, answerMaxLength - 3) + "..."
      else
        text
    }
  }
}
