package edu.emory.mathcs.ir.liveqa

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.Stat
import com.twitter.finagle.{Http, Service}
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, FuturePool}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.base._
import edu.emory.mathcs.ir.liveqa.crowd.{CrowdApi, CrowdQuestionAnswerer}
import edu.emory.mathcs.ir.liveqa.ranking.{RanklibModelRanker, ScoringBasedRanking}
import edu.emory.mathcs.ir.liveqa.scoring.TermOverlapAnswerScorer
import edu.emory.mathcs.ir.liveqa.scoring.features._
import edu.emory.mathcs.ir.liveqa.util.LogFormatter
import edu.emory.mathcs.ir.liveqa.verticals.answerscom.AnswersComCandidateGenerator
import edu.emory.mathcs.ir.liveqa.verticals.web.WebSearchCandidateGenerator
import edu.emory.mathcs.ir.liveqa.verticals.wikihow.WikiHowCandidateGenerator
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

  // List of query generators.
  val featureGenerator = new MergeFeatures(
    new Bm25Features, new AnswerStatsFeatures, new TermOverlapFeatures, new MatchesFeatures, new SourceFeatures
  )

  // Candidate answer ranking module.
  val ranker = RanklibModelRanker.create(cfg.getString("qa.rank.model"),
    featureGenerator, cfg.getString("qa.rank.alphabet"))

  val queryGenerator = new CombineQueriesGenerator(new TitleQueryGeneration, new Top5IdfQueryGenerator, new LongestQuestionQueryGenerator)
  val webQueryGenerator = new CombineQueriesGenerator(new TitleQueryGeneration, new Top5IdfQueryGenerator)

  // Question answering module.
  val questionAnswerer = getQuestionAnswerer

  private def getQuestionAnswerer: TextQuestionAnswerer = {
    if (cfg.getBoolean("qa.crowd.enabled"))
      new CrowdQuestionAnswerer(
        new MergingCandidateGenerator(
          new YahooAnswerCandidateGenerator(queryGenerator),
          new AnswersComCandidateGenerator(queryGenerator),
          new WikiHowCandidateGenerator(queryGenerator),
          new WebSearchCandidateGenerator(webQueryGenerator))
        , ranker)
    else
      new TextQuestionAnswerer(
        new MergingCandidateGenerator(
          new YahooAnswerCandidateGenerator(queryGenerator),
          new AnswersComCandidateGenerator(queryGenerator),
          new WikiHowCandidateGenerator(queryGenerator),
          new WebSearchCandidateGenerator(webQueryGenerator)
        ), ranker
      )
  }

  def respond(qid:String, category:String, title:String, body:Option[String]) = {
    FuturePool.unboundedPool {
      Stat.time(answerLatency) {
        // Log the question.
        val answerReceivedTime = DateTime.now.toInstant.getMillis

        logger.info(LogFormatter("QUESTION", Array(qid, title, body.getOrElse(""))))
        val question = new Question(qid, category, title, body, DateTime.now)

        // Generate the answer.
        val answer = questionAnswerer.answer(question)
        answer.time = DateTime.now.toInstant.getMillis - answerReceivedTime

        // Log and return the answer.
        logger.info(LogFormatter("ANSWER", Array(answer.answer, answer.sources.headOption.getOrElse("No sources"))))
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
