package edu.emory.mathcs.ir.liveqa.base

import io.finch.EncodeResponse

/**
  * Answer response, which contains the actual text of the answer, sources used
  * to generate the answer and some additional information expected by the TREC
  * LiveQA organizers.
  */
class Answer(val answer: String, val sources: Array[String]) {

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
}