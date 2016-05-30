package edu.emory.mathcs.ir.liveqa.base

import com.typesafe.config.ConfigFactory
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.finch.EncodeResponse

/**
  * Answer response, which contains the actual text of the answer, sources used
  * to generate the answer and some additional information expected by the TREC
  * LiveQA organizers.
  */
case class Answer(id:Int, qid: String, answer: String, sources: Array[String]) {
  var time: Long = 0

  /**
    * Alternative constructor, that doesn't take id as a parameter.
    * @param answer The answer to the question.
    * @param sources The sources used to generate the answer.
    */
  def this(qid: String, answer:String, sources: Array[String]) = this(0, qid, answer, sources)

  /**
    * Encodes the answer in XML format.
    * @return XML with answer representation. XML format is the one expected in
    *         response from TREC LiveQA organizers.
    */
  def toXml =
    answer
    <xml>
      <answer pid={Answer.systemName} answered="yes" time={time.toString} qid={qid}>
        <content>{answer}</content>
        <resources>{sources}</resources>
        <title-foci></title-foci>
        <body-foci></body-foci>
        <summary></summary>
      </answer>
    </xml>

}

object Answer {
  val cfg = ConfigFactory.load()
  val systemName = cfg.getString("qa.system_name")

  implicit val ee: EncodeResponse[Answer] =
    EncodeResponse.fromString("application/xml") {
      answer => answer.toXml.toString
    }

  implicit val esa: EncodeResponse[Seq[Answer]] =
    EncodeResponse.fromString("application/json") (_.asJson.noSpaces)
}