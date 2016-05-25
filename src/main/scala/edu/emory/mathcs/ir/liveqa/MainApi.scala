package edu.emory.mathcs.ir.liveqa

import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.{Request, Response}
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
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.CROWD
import edu.emory.mathcs.ir.liveqa.base.{Answer, MergingCandidateGenerator, Question}
import edu.emory.mathcs.ir.liveqa.crowd.{CrowdDb, CrowdQuestionAnswerer}
import edu.emory.mathcs.ir.liveqa.util.LogFormatter
import edu.emory.mathcs.ir.liveqa.verticals.web.WebSearchCandidateGenerator
import edu.emory.mathcs.ir.liveqa.verticals.yahooanswers.YahooAnswerCandidateGenerator
import io.finch._
import org.joda.time.DateTime

/**
  * Created by dsavenk on 4/19/16.
  */
object MainApi extends TwitterServer with LazyLogging {
  private val cfg = ConfigFactory.load()
  val port = cfg.getString("port")
  val answerLatency: Stat = statsReceiver.stat("answer_latency")

  private val liveQaParams = param("qid") :: param("category") ::
    param("title") :: paramOption("body")
  val getEndpoint: Endpoint[Answer] = get(liveQaParams)(respond(_,_,_,_))
  val postEndpoint: Endpoint[Answer] = post(liveQaParams)(respond(_,_,_,_))
  val currentQuestionEndpoint: Endpoint[Question] = get("question")(getCurrentQuestion)
  val workerAnswerEndpoint: Endpoint[Unit] = get("worker_answer" :: param("qid") :: param("answer") :: param("worker"))(addWorkerAnswer(_,_,_))
  val getAnswersEndpoint: Endpoint[Seq[Answer]] = get("get_answers" :: param("qid") :: param("worker"))(getAnswersToRate(_,_))
  val rateAnswerEndpoint: Endpoint[Unit] = get("rate_answer" :: param("aid") :: param("worker") :: param("rating"))(rateAnswer(_,_,_))

  // TODO(denxx): I should make this more restrictive and allow requests from our servers only.
  // This is to let cross-domain requests.
  val corsFilter = new Cors.HttpFilter(Cors.UnsafePermissivePolicy)
  val api: Service[Request, Response] = corsFilter andThen
    (getEndpoint :+: postEndpoint :+: currentQuestionEndpoint :+: workerAnswerEndpoint :+: getAnswersEndpoint :+: rateAnswerEndpoint).toService

  // Question answering module.
  val questionAnswerer =
    if (cfg.getBoolean("qa.crowd.enabled"))
      new CrowdQuestionAnswerer(
        new MergingCandidateGenerator(
          new YahooAnswerCandidateGenerator //,new WebSearchCandidateGenerator)
    ))
    else
      new TextQuestionAnswerer(
        new MergingCandidateGenerator(
          new YahooAnswerCandidateGenerator,
          new WebSearchCandidateGenerator)
      )

  def main(): Unit = {
    val server = Http.server
      .configured(Stats(statsReceiver))
//      .withTls(Netty3ListenerTLSConfig(() =>
//        Ssl.server(cfg.getString("ssl.certificate"), cfg.getString("ssl.key"),
//          null, null, null)))
      .serve(port, api)

    onExit { server.close() }

    Await.ready(adminHttpServer)
  }

  def respond(qid:String, category:String, title:String, body:Option[String]) = {
    FuturePool.unboundedPool {
      Stat.time(answerLatency) {
        // Log the question.
        logger.info(LogFormatter("QUESTION", Array(qid, title, body.getOrElse(""))))
        val question = new Question(qid, category, title, body, DateTime.now)

        // Generate the answer.
        val answer = questionAnswerer.answer(question)

        // Log and return the answer.
        logger.info(LogFormatter("ANSWER", Array(answer.answer)))
        Ok(answer)
      }
    }
  }

  /**
    * @return Returns the current question in json format.
    */
  def getCurrentQuestion = {
    val question = CrowdDb.currentQuestion
    if (question.isDefined) Ok(question.get)
    else NotFound(new Exception("No pending questions."))
  }

  /**
    * Add the answer received from mechanical turk to the database.
    * @param qid Qid of the current question.
    * @param answer The answer submitted by a mechanical turk worker.
    * @return Ok
    */
  def addWorkerAnswer(qid:String, answer:String, worker:String) = {
    CrowdDb.addAnswer(qid, answer, "Mechanical Turk", worker, CROWD)
    Ok()
  }

  def getAnswersToRate(qid: String, worker: String) = {
    Ok(CrowdDb.getAnswers(qid, worker))
  }

  def rateAnswer(aid: String, worker: String, rating: String) = {
    CrowdDb.rateAnswer(aid.toInt, worker, rating.toInt)
    Ok()
  }
}
