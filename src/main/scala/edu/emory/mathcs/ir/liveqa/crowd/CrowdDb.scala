package edu.emory.mathcs.ir.liveqa.crowd

import com.typesafe.scalalogging.LazyLogging
import edu.emory.mathcs.ir.liveqa.base.{Answer, AnswerCandidate, Question}
import java.sql.Timestamp

import com.typesafe.config.ConfigFactory
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate._
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

  // Define additional elements for the enumeration objects.
  case object CROWD extends AnswerType
  case object CrowdRating extends CandidateAttribute

  // Database connection.
  val db = Database.forConfig("qa.crowd.db")

  /**
    * Converter for the DateTime type to the database type.
    */
  implicit lazy val jodaType = MappedColumnType.base[DateTime, Timestamp](
    {d => new Timestamp(d.getMillis)} ,
    {d => new DateTime(d.getTime, UTC)}
  )

  class Workers(tag: Tag) extends Table[(Int, String, String, String, DateTime, Option[DateTime])](tag, "WORKERS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def workerId = column[String]("WORKER_ID")
    def assignmentId = column[String]("ASSIGNMENT_ID")
    def hitId = column[String]("HIT_ID")
    def start = column[DateTime]("STARTED")
    def finish = column[Option[DateTime]]("FINISHED")

    def * = (id, workerId, assignmentId, hitId, start, finish)
    def assignment_id_index = index("idx_assignment_id", assignmentId, unique = false)
  }
  val workers = TableQuery[Workers]

  /**
    * Class defining the structure of the questions table.
    */
  class Questions(tag: Tag) extends Table[(String, String, String, String, DateTime, Boolean)](tag, "QUESTIONS") {
    def qid = column[String]("QID", O.PrimaryKey)
    def category = column[String]("CATEGORY")
    def title = column[String]("TITLE")
    def body = column[String]("BODY")
    def received = column[DateTime]("RECEIVED")
    def answered = column[Boolean]("ANSWERED")

    def * = (qid, category, title, body, received, answered)
  }
  // Slick table query for the questions table.
  val questions = TableQuery[Questions]

  /**
    * Class defining the structure of the answers table.
    */
  class Answers(tag: Tag) extends Table[(Int, String, String, String, String, DateTime, Int, Int)](tag, "ANSWERS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def qid = column[String]("QID")
    def answer = column[String]("ANSWER")
    def source = column[String]("SOURCE")
    def worker = column[String]("WORKERID", O.Default(""))
    def created = column[DateTime]("CREATED")
    def rank = column[Int]("RANK", O.Default(-1))
    def answerType = column[Int]("ANSWER_TYPE")

    def * = (id, qid, answer, source, worker, created, rank, answerType)
    // Foreign key to link answers to the questions.
    def questionId = foreignKey("QUESTION_FK", qid, questions)(_.qid)
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
    val setup = (workers.schema ++ questions.schema ++
      answers.schema ++ ratings.schema).create

    val setupFuture = db.run(setup)
    setupFuture onFailure  {
      case exc: Throwable => logger.error(exc.getMessage)
    }
    Await.result(setupFuture, Duration.Inf)
  }


  /**
    * Adds a new worker task to the database.
    * @param workerId MTurk worker id.
    * @param assignmentId Mturk assignment id.
    * @param hitId Mturk hit id.
    */
  def addWorkerTask(workerId: String, assignmentId: String, hitId: String): Future[_] = {
    db.run(workers += (0, workerId, assignmentId, hitId, DateTime.now, None))
  }

  /**
    * Finishes the given assignment by posting its finish time to the database.
    * @param assignmentId The id of the assignment to finish.
    */
  def finishWorkerTask(assignmentId: String): Future[_] = {
    db run {
      workers.filter(_.assignmentId === assignmentId)
        .map(_.finish)
        .update(Some(DateTime.now))
    }
  }

  /**
    * Adds question to the database.
    *
    * @param question Question to add to the database.
    * @return A Future with the database query status.
    */
  def postQuestion(question: Question): Future[_] = {
      db.run(questions += (question.qid, question.category, question.title,
        question.body.getOrElse(""), question.submittedTime, false))
  }

  /**
    * Returns the list of questions posted (and not yet answered) between the
    * given time stamps.
    * @param from Lower bound on time a question should be posted.
    * @param to Upper bound on time a question should be posted.
    * @return A sequence of non-answered questions in the given time span.
    */
  def getQuestions(from: DateTime, to:DateTime): Seq[Question] = {
    val questionFuture = db.run(
      questions.filter(q => q.received >= from && q.received <= to && !q.answered)
        .sortBy(q => q.received)
        .result).map(_.map {
          case (qid, category, title, body, time, answered) =>
            new Question(qid, category, title, Some(body), time)
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
    val questions = getQuestions(now.minusSeconds(cfg.getInt("qa.timeout") - cfg.getInt("qa.crowd.time_gap")), now)
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
    * Adds the answer to the question with the given qid to the database.
    *
    * @param qid Qid of the question to answer.
    * @param answer The answer to add.
    */
  def addAnswer(qid: String, answer: String, source: String, worker: String, answerType: AnswerType) = {
    db.run(answers += (0, qid, answer, source, worker, DateTime.now, -1, getAnswerTypeId(answerType)))
  }

  /**
    * Adds all the answers from the list to the database.
    *
    * @param a A list of answers.
    * @return A future of the database query.
    */
  def addAnswers(a: Seq[(String, String, String, String, Int, AnswerType)]) = {
    val futureQuery = db.run(
      answers ++= a.map(answer => (0, answer._1, answer._2, answer._3,
        answer._4, DateTime.now, answer._5, getAnswerTypeId(answer._6))))
    futureQuery.onFailure {
      case e: Exception => logger.error(e.getMessage)
    }
    futureQuery
  }

  /**
    * Returns the answers to the given question, that haven't been rated yet
    * by the current worker.
    *
    * @param qid Qid of the current question.
    * @param worker The id of the worker.
    * @return A list of answers, not rated by the current worker.
    */
  def getAnswers(qid: String, worker: String): Seq[Answer] = {
    val answerRatingsQuery = answers
      .filter(a => (a.qid === qid) && (a.worker =!= worker))
      .joinLeft(ratings.filter(_.worker === worker)) on (_.id === _.aid)

    val answerRatings = Await.result(
      db.run(answerRatingsQuery.result), Duration.Inf)
    answerRatings.filterNot(_._2.isDefined)
      .map(a => new Answer(a._1._1, a._1._3, Array(a._1._4)))
  }

  /**
    * Save the rating for the given answer.
    *
    * @param aid Id of the answer to rate.
    * @param worker Id of the worker, who rated the answer.
    * @param rating The rating of the answer.
    */
  def rateAnswer(aid: Int, worker: String, rating: Int): Unit = {
    db.run(ratings += (0, aid, rating, worker))
  }

  /**
    * Retrieves answer ratings and returns a mapping from answer to its
    * ratings.
    *
    * @param qid Qid of the question.
    * @return A mapping from candidate answer to its ratings.
    */
  def getRatedAnswers(qid: String): Map[String, Seq[Int]] = {
    val answerRatings = Await.result(
      db.run((answers.filter(_.qid === qid)
        joinLeft ratings
        on (_.id === _.aid))
        .result),
      Duration.Inf)

    // Group by answer (its rank) and return a map from candidate rank to its ratings.
    answerRatings.groupBy(_._1).map {
      case (key, value) =>
        (key._3, value.filter(_._2.isDefined).map(_._2.get._3))
    }
  }

  /**
    * Returns a list of answers created by mechanical turk workers for the given
    * question.
    *
    * @param qid QID of the question to retrive worker answers for.
    * @return A list of [[AnswerCandidate]] generated by Mechanical Turk workers.
    */
  def getWorkerAnswer(qid: String): Seq[AnswerCandidate] = {
    val workerAnswers = Await.result(
      db.run(answers.filter(a => (a.qid === qid) && (a.worker =!= "")).result),
      Duration.Inf)
    workerAnswers.map(a => new AnswerCandidate(CROWD, a._3, a._4))
  }

  // TODO(denxx): This is bad, need to come up with a better way to map from
  // answer type to integer ids to store in DB.
  def getAnswerTypeId(answerType: AnswerType): Int = answerType match {
    case YAHOO_ANSWERS => 1
    case WEB => 2
    case ANSWERS_COM => 3
    case CROWD => 4
    case _ => -1
  }

  def getAnswerTypeById(answerType: Int): AnswerType = answerType match {
    case 1 => YAHOO_ANSWERS
    case 2 => WEB
    case 3 => ANSWERS_COM
    case 4 => CROWD
    case _ => WEB
  }

  /**
    * Main method to test DB.
    */
  def main(args: Array[String]): Unit = {
    CrowdDb.createDb()
    //    CrowdDb.getCurrentQuestion.foreach(q => println(q.title))
    //db.run(questions += ("1", "Health & Diet", "Who is John Galt? Who is John Galt? Who is John Galt? Who is John Galt? Who is John Galt? Who is John Galt? ", "I've heard this phrase a lot, but don't know what it means... I've heard this phrase a lot, but don't know what it means...I've heard this phrase a lot, but don't know what it means...I've heard this phrase a lot, but don't know what it means...", DateTime.now(), false))
    db.close()
  }
}