package edu.emory.mathcs.ir.liveqa

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.Stat
import com.twitter.finagle.{Http, Service}
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, FuturePool}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.base.{Answer, MergingCandidateGenerator, Question}
import edu.emory.mathcs.ir.liveqa.crowd.{CrowdApi, CrowdQuestionAnswerer}
import edu.emory.mathcs.ir.liveqa.ranking.ScoringBasedRanking
import edu.emory.mathcs.ir.liveqa.scoring.TermOverlapAnswerScorer
import edu.emory.mathcs.ir.liveqa.util.LogFormatter
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

  val api: Service[Request, Response] =(getEndpoint :+: postEndpoint).toService

  // Question answering module.
  val questionAnswerer = getQuestionAnswerer

  private def getQuestionAnswerer: TextQuestionAnswerer = {
    if (cfg.getBoolean("qa.crowd.enabled"))
      new CrowdQuestionAnswerer(
        new MergingCandidateGenerator(
          new YahooAnswerCandidateGenerator //,new WebSearchCandidateGenerator)
        ), new ScoringBasedRanking(new TermOverlapAnswerScorer))
    else
      new TextQuestionAnswerer(
        new MergingCandidateGenerator(
          new YahooAnswerCandidateGenerator //,new WebSearchCandidateGenerator
        ), new ScoringBasedRanking(new TermOverlapAnswerScorer)
      )
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

  def main(): Unit = {
    val server = Http.server
      .configured(Stats(statsReceiver))
      .serve(port, api)
    onExit { server.close() }

    if (cfg.getBoolean("qa.crowd.enabled")) {
      val mturkServer = CrowdApi.start(cfg.getString("qa.crowd.port"))
      onExit { mturkServer.close() }
    }

    Await.ready(adminHttpServer)
  }
}
