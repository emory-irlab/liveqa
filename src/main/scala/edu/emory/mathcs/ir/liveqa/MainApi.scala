package edu.emory.mathcs.ir.liveqa

import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.{Request, Response, TlsFilter}
import com.twitter.finagle.netty3.Netty3ListenerTLSConfig
import com.twitter.finagle.param.Stats
import com.twitter.finagle.ssl.Ssl
import com.twitter.finagle.stats.Stat
import com.twitter.finagle.{Http, Service}
import com.twitter.logging.config
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, FuturePool}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.base.{Answer, Question}
import edu.emory.mathcs.ir.liveqa.crowd.CrowdDb
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
  val currentQuestionApi: Endpoint[Question] = get("question")(getCurrentQuestion)

  // TODO(denxx): I should make this more restrictive and allow requests from our servers only.
  // This is to let cross-domain requests.
  val corsFilter = new Cors.HttpFilter(Cors.UnsafePermissivePolicy)
  val api: Service[Request, Response] = corsFilter andThen
    (getApi :+: postApi :+: currentQuestionApi).toService

  def main(): Unit = {
    val server = Http.server
      .configured(Stats(statsReceiver))
      .withTls(Netty3ListenerTLSConfig(() =>
        Ssl.server("src/main/resources/ssl/carbonite.crt",
          "src/main/resources/ssl/carbonite.key", null, null, null)))
      .serve(port, api)

    onExit { server.close() }

    Await.ready(adminHttpServer)
  }

  def respond(qid:String, category:String, title:String, body:String) = {
    FuturePool.unboundedPool {
      Stat.time(answerLatency) {
        // Log the question.
        logger.info(LogFormatter("QUESTION", Array(qid, title, body)))
        val question = new Question(qid, category, title,
          if (body.trim.isEmpty) None else Some(body))

        if (cfg.getBoolean("use_crowd")) {
          CrowdDb.postQuestion(question)
        }

        // Generate the answer.
        val answer = QuestionAnswerer(question)

        // Log and return the answer.
        logger.info(LogFormatter("ANSWER", Array(answer.answer)))

        // Setting the answered status for the question.
        if (cfg.getBoolean("use_crowd")) {
          CrowdDb.setAnswered(question)
        }

        Ok(answer)
      }
    }
  }

  def getCurrentQuestion = {
    val question = CrowdDb.currentQuestion
    if (question.isDefined) Ok(question.get)
    else NotFound(new Exception("No pending questions."))
  }
}
