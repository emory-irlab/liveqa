package edu.emory.mathcs.ir.liveqa.base

import edu.stanford.nlp.simple.Document
import io.circe.{Encoder, Json}
import io.finch.EncodeResponse

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

object Question {
  implicit val ee: EncodeResponse[Question] =
    EncodeResponse.fromString("application/json") { q =>
      Json.obj(
        "qid" -> Json.string(q.qid),
        "category" -> Json.string(q.category),
        "title" -> Json.string(q.title),
        "body" -> Json.string(q.body.getOrElse(""))).toString
    }
}