package edu.emory.mathcs.ir.liveqa.base

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.finch.EncodeResponse

/**
  * Answer response, which contains the actual text of the answer, sources used
  * to generate the answer and some additional information expected by the TREC
  * LiveQA organizers.
  */
case class Answer(id:Int, answer: String, sources: Array[String]) {

  /**
    * Alternative constructor, that doesn't take id as a parameter.
    * @param answer The answer to the question.
    * @param sources The sources used to generate the answer.
    */
  def this(answer:String, sources: Array[String]) = this(0, answer, sources)

  /**
    * Encodes the answer in XML format.
    * @return XML with answer representation. XML format is the one expected in
    *         response from TREC LiveQA organizers.
    */
  def toXml =
    <xml>
      <answer pid="emory-irlab" answered="yes" time="">
        <content>{answer}</content>
        <resources>{sources}</resources>
        <title-foci></title-foci>
        <body-foci></body-foci>
        <summary></summary>
      </answer>
    </xml>

}

object Answer {
  implicit val ee: EncodeResponse[Answer] =
    EncodeResponse.fromString("application/xml") {
      answer => answer.toXml.toString
    }

  implicit val esa: EncodeResponse[Seq[Answer]] =
    EncodeResponse.fromString("application/json") (_.asJson.noSpaces)
}