package edu.emory.mathcs.ir.liveqa

import com.twitter.finagle.Http
import com.twitter.util.Await
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.util.LogFormatter
import io.finch._

/**
  * Created by dsavenk on 4/19/16.
  */
object MainApi extends App with LazyLogging {

  val liveQaApi: Endpoint[Answer] = get(param("qid") :: param("category") ::
                                        param("title") :: param("body") ) {
    (qid:String, category:String, title:String, body:String) =>

      // Log the question.
      logger.info(LogFormatter("QUESTION", Array(qid, title, body)))

      // Generate the answer.
      val answer = QuestionAnswerer(new Question(qid, category, title,
        if (body.trim.isEmpty) None else Some(body)))

      // Log and return the answer.
      logger.info(LogFormatter("ANSWER", Array(answer.answer)))
      Ok(answer)
  }

  Await.ready(Http.serve(":8080", liveQaApi.toService))
}
