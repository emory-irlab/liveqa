package edu.emory.mathcs.ir.liveqa

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.Stat
import com.twitter.finagle.{Http, Service}
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, FuturePool}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.util.LogFormatter
import io.finch._

/**
  * Created by dsavenk on 4/19/16.
  */
object MainApi extends TwitterServer with LazyLogging {
  private val cfg = ConfigFactory.load()
  val port = cfg.getString("port")
  val answerLatency: Stat = statsReceiver.stat("answer_latency")

  private val liveQaParams = param("qid") :: param("category") ::
    param("title") :: param("body")
  val getApi: Endpoint[Answer] = get(liveQaParams)(respond(_,_,_,_))
  val postApi: Endpoint[Answer] = post(liveQaParams)(respond(_,_,_,_))

  val api: Service[Request, Response] = (getApi :+: postApi).toService

  def main(): Unit = {
    val server = Http.server
      .configured(Stats(statsReceiver))
      .serve(port, api)

    onExit { server.close() }

    Await.ready(adminHttpServer)
  }

  def respond(qid:String, category:String, title:String, body:String) = {
    FuturePool.unboundedPool {
      Stat.time(answerLatency) {
        // Log the question.
        logger.info(LogFormatter("QUESTION", Array(qid, title, body)))

        // Generate the answer.
        val answer = QuestionAnswerer(new Question(qid, category, title,
          if (body.trim.isEmpty) None else Some(body)))

        // Log and return the answer.
        logger.info(LogFormatter("ANSWER", Array(answer.answer)))
        Ok(answer)
      }
    }
  }
}
