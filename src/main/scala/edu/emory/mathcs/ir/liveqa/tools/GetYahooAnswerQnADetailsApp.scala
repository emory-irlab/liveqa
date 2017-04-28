package edu.emory.mathcs.ir.liveqa.tools

import java.io.{File, PrintWriter}
import java.net.URI
import java.util.concurrent.TimeUnit

import cats.syntax.either._
import com.twitter.util.{Await, Duration}
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import edu.emory.mathcs.ir.liveqa.verticals.yahooanswers.YahooAnswersQuestion

import scala.io.Source

case class InputEntity(name: String, score: Float, mid: String, surface_score: Float, position: List[Int])
case class InputQnA(question: String, question_entities: Option[List[InputEntity]],
                    source: String, answer: String,
                    link:String, answer_entities: Option[List[InputEntity]])

case class OutputQnA(question: String, body: String, question_entities: List[InputEntity], source: String,
                     answer: String, answer_entities: List[InputEntity], qid:String, categories: List[String],
                     answers: List[String])

/**
  * Created by dsavenk on 3/10/17.
  */
object GetYahooAnswerQnADetailsApp {

  def processJson(questions: Array[InputQnA]): Array[OutputQnA] = {
    questions map {
      q =>
        val qid = q.link.split("qid=")(1)
        Await.result(YahooAnswersQuestion(qid), Duration(1, TimeUnit.MINUTES)).map {
          yaq =>
            print(".")
            OutputQnA(q.question, yaq.body, q.question_entities.getOrElse(Nil), q.source, q.answer,
              q.answer_entities.getOrElse(Nil), qid,
              yaq.categories.toList, yaq.answers.toList)
        }.getOrElse(OutputQnA(q.question, "", q.question_entities.getOrElse(Nil), q.source, q.answer,
          q.answer_entities.getOrElse(Nil), qid, Nil, Nil))
    }
  }

  def main(args: Array[String]): Unit = {
    val content = Source.fromFile(args(0)).mkString
//    val questions = decode[List[InputQnA]](content)
//    val res = if (questions.isLeft) questions.left else questions.right
    val out = new PrintWriter(new File(args(1)))
    decode[Array[InputQnA]](content) match {
      case Left(failure) =>
        println(failure)
      case Right(questions) =>
        out.write(processJson(questions).asJson.spaces2)
    }
    out.close()
  }
}
