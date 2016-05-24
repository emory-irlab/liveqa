package edu.emory.mathcs.ir.liveqa.base

import com.typesafe.config.ConfigFactory
import edu.stanford.nlp.simple.Document
import io.circe.Json
import io.finch.EncodeResponse
import org.joda.time.{DateTime, Seconds}

/**
  * A question from Yahoo! Answers submitted to the system.
  */
case class Question(qid: String,
                    category:String,
                    title: String,
                    body: Option[String],
                    submittedTime: DateTime) {
  val titleNlp = new Document(title)
  val bodyNlp = new Document(body.getOrElse(""))
}

object Question {
  val cfg = ConfigFactory.load()

  implicit val ee: EncodeResponse[Question] =
    EncodeResponse.fromString("application/json") { q =>
      Json.obj(
        "qid" -> Json.string(q.qid),
        "category" -> Json.string(q.category),
        "title" -> Json.string(q.title),
        "body" -> Json.string(q.body.getOrElse("")),
        "submittedTime" -> Json.string(q.submittedTime.toString),
        "timeLeft" -> Json.int(math.max(0,
          cfg.getInt("qa.timeout") - Seconds.secondsBetween(
            q.submittedTime, DateTime.now()).getSeconds))
      ).toString
    }
}