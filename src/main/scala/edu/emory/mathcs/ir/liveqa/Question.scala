package edu.emory.mathcs.ir.liveqa

/**
  * A question from Yahoo! Answers submitted to the system.
  */
case class Question(qid: String, category:String, title: String, body: Option[String])
