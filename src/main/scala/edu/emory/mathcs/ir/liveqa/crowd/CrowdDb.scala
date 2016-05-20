package edu.emory.mathcs.ir.liveqa.crowd

import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.base.Question
import java.sql.Timestamp

import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import slick.driver.SQLiteDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Database for the interaction with the crowdsourcing module.
  */
object CrowdDb extends LazyLogging {
  private val cfg = ConfigFactory.load()

  // Database connection.
  val db = Database.forConfig("crowdb_sqlite")

  /**
    * Converter for the DateTime type to the database type.
    */
  implicit lazy val jodaType = MappedColumnType.base[DateTime, Timestamp](
    {d => new Timestamp(d.getMillis)} ,
    {d => new DateTime(d.getTime, UTC)}
  )

  /**
    * Class defining the structure of the questions table.
    */
  class Questions(tag: Tag) extends Table[(Int, String, String, String, String, DateTime, Boolean)](tag, "QUESTIONS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def qid = column[String]("QID")
    def category = column[String]("CATEGORY")
    def title = column[String]("TITLE")
    def body = column[String]("BODY")
    def received = column[DateTime]("RECEIVED")
    def answered = column[Boolean]("ANSWERED")

    def * = (id, qid, category, title, body, received, answered)
  }
  // Slick table query for the questions table.
  val questions = TableQuery[Questions]

  /**
    * Class defining the structure of the answers table.
    */
  class Answers(tag: Tag) extends Table[(Int, Int, String, String, DateTime)](tag, "ANSWERS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def qid = column[Int]("QID")
    def answer = column[String]("ANSWER")
    def source = column[String]("SOURCE")
    def created = column[DateTime]("CREATED")

    def * = (id, qid, answer, source, created)
    // Foreign key to link answers to the questions.
    def questionId = foreignKey("QUESTION_FK", qid, questions)(_.id)
    def qidIndex = index("idx_qid", qid)
  }
  val answers = TableQuery[Answers]

  /**
    * Class defining the structure of the answer rating table.
    */
  class AnswerRatings(tag: Tag) extends Table[(Int, Int, Int, String)](tag, "RATINGS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def aid = column[Int]("AID")
    def rating = column[Int]("RATING")
    def worker = column[String]("WORKER")

    def * = (id, aid, rating, worker)
    // Foreign key to the answers table.
    def answerId = foreignKey("ANSWER_FK", aid, answers)(_.id)
  }
  val ratings = TableQuery[AnswerRatings]

  /**
    * Creates required databases.
    */
  def createDb(): Unit = {
    val setup = (questions.schema ++ answers.schema ++ ratings.schema).create

    val setupFuture = db.run(setup)
    setupFuture onFailure  {
      case exc: Throwable => logger.error(exc.getMessage)
    }
    Await.result(setupFuture, Duration.Inf)
  }

  /**
    * Adds question to the database.
    *
    * @param question Question to add to the database.
    * @return A Future with the database query status.
    */
  def postQuestion(question: Question): Future[_] = {
      db.run(questions += (0, question.qid, question.category, question.title,
        question.body.getOrElse(""), DateTime.now(), false))
  }

  def getQuestions(from: DateTime, to:DateTime): Seq[Question] = {
    val questionFuture = db.run(
      questions.filter(q => q.received >= from && q.received <= to && !q.answered)
        .sortBy(q => q.received)
        .result).map(_.map {
          case (id, qid, category, title, body, time, answered) =>
            new Question(qid, category, title, Some(body))
        })
    Await.result(questionFuture, Duration.Inf)
  }

  /**
    * Returns the question that is currently active (received within a minute
    * and wasn't answered) or None.
    *
    * @return Current question or None if there are no pending questions.
    */
  def currentQuestion: Option[Question] = {
    val now = DateTime.now
    val questions = getQuestions(now.minusSeconds(cfg.getInt("qa.timeout")), now)
    questions.lastOption
  }

  /**
    * Sets the answered flag on the given question.
    *
    * @param question The question to set the flag for. Actually, the method
    *                 only uses question's QID.
    * @return The future of the database query status.
    */
  def setAnswered(question : Question): Future[_] = {
    db.run(questions
      .filter(q => q.qid === question.qid)
      .map(q => q.answered)
      .update(true))
  }

  /**
    * Main method to test DB.
    */
  def main() {
    //CrowdDb.createDb()
    //    CrowdDb.getCurrentQuestion.foreach(q => println(q.title))
    //    CrowdDb.setAnswered(new Question("QID", "", "", Some("")))
  }
}