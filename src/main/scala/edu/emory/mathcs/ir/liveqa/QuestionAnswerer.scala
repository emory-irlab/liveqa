package edu.emory.mathcs.ir.liveqa

import edu.emory.mathcs.ir.liveqa.yahooanswers.Search
import com.twitter.util.Await

/**
  * The main question answering object, that maps a question into the answer.
  */
object QuestionAnswerer {
  def apply(question: Question): Answer = {
    val res = Await.result(Search(question.title))
    new Answer(res.head.answers.head, Array(res.head.qid))
  }
}
