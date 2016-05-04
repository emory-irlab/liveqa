package edu.emory.mathcs.ir.liveqa.base

import edu.stanford.nlp.simple.Document

/**
  * A question from Yahoo! Answers submitted to the system.
  */
case class Question(qid: String,
                    category:String,
                    title: String,
                    body: Option[String]) {
  val titleNlp = new Document(title)
  val bodyNlp = new Document(body.getOrElse(""))
}