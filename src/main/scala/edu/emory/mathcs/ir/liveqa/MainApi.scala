package edu.emory.mathcs.ir.liveqa

import com.twitter.finagle.Http
import com.twitter.util.Await
import io.finch._

/**
  * Created by dsavenk on 4/19/16.
  */
object MainApi extends App {

  val liveQaApi: Endpoint[Answer] = get(param("qid") :: param("category") ::
                                        param("title") :: param("body") ) {
    (qid:String, category:String, title:String, body:String) =>
      Ok(QuestionAnswerer(new Question(qid, category, title,
        if (body.trim.isEmpty) None else Some(body))))
  }

  Await.ready(Http.serve(":8080", liveQaApi.toService))
}
