package edu.emory.mathcs.ir.liveqa.crowd

import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.netty3.Netty3ListenerTLSConfig
import com.twitter.finagle.ssl.Ssl
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.base.{Answer, Question}
import edu.emory.mathcs.ir.liveqa.crowd.CrowdDb.CROWD
import io.finch._

/**
  * Created by dsavenk on 5/26/16.
  */
object CrowdApi extends LazyLogging {
  val cfg = ConfigFactory.load

  // Endpoints for the API that is used from mturk external HIT page.
  val addWorkerTaskEndpoint: Endpoint[Unit] = get("add_task" :: param("workerId") :: param("assignmentId") :: param("hitId"))(addWorkerTask(_,_,_))
  val finishWorkerTaskEndpoint: Endpoint[Unit] = get("finish_task" :: param("assignmentId"))(finishWorkerTask(_))
  val currentQuestionEndpoint: Endpoint[Question] = get("question")(getCurrentQuestion)
  val workerAnswerEndpoint: Endpoint[Unit] = get("worker_answer" :: param("qid") :: param("answer") :: param("worker"))(addWorkerAnswer(_,_,_))
  val getAnswersEndpoint: Endpoint[Seq[Answer]] = get("get_answers" :: param("qid") :: param("worker"))(getAnswersToRate(_,_))
  val rateAnswerEndpoint: Endpoint[Unit] = get("rate_answer" :: param("aid") :: param("worker") :: param("rating"))(rateAnswer(_,_,_))

  // TODO(denxx): I should make this more restrictive and allow requests from our servers only.
  // This is to let cross-domain requests.
  val corsFilter = new Cors.HttpFilter(Cors.UnsafePermissivePolicy)
  val mturkApi : Service[Request, Response] = corsFilter andThen (
      addWorkerTaskEndpoint :+:
      finishWorkerTaskEndpoint :+:
      currentQuestionEndpoint :+:
      workerAnswerEndpoint :+:
      getAnswersEndpoint :+:
      rateAnswerEndpoint
    ).toService

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

  def addWorkerTask(workerId: String, assignmentId: String, hitId: String) = {
    CrowdDb.addWorkerTask(workerId, assignmentId, hitId)
    Ok()
  }

  def finishWorkerTask(assignmentId: String) = {
    CrowdDb.finishWorkerTask(assignmentId)
    Ok()
  }

  def start(port: String): ListeningServer = {
    Http.server
      .withTls(Netty3ListenerTLSConfig(() =>
        Ssl.server(cfg.getString("ssl.certificate"), cfg.getString("ssl.key"),
          null, null, null)))
      .serve(port, mturkApi)
  }
}
