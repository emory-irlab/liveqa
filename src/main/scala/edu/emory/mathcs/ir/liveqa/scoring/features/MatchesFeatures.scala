package edu.emory.mathcs.ir.liveqa.scoring.features

import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.{QuestionBody, QuestionTitle}
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import edu.emory.mathcs.ir.liveqa.util.NlpUtils

/**
 * Created by dsavenk on 5/28/16.
 */
class MatchesFeatures extends FeatureCalculation {

  def getTermMatchFeatures(questionTerms: Seq[String], answerTerms: Seq[String], suffix: String): Map[String, Float] = {
    val questionLength = questionTerms.size
    val questionTermsSet = questionTerms.toSet

    // Compute longest span of matched terms.
    var longestSpan = 0
    var curLength = 0
    for (answerTerm <- answerTerms) {
      if (questionTermsSet.contains(answerTerm)) {
        curLength += 1
        longestSpan = math.max(longestSpan, curLength)
      }
      else curLength = 0
    }

    if (questionTerms.nonEmpty) {
      Map(
        "MatchedTerms%" + suffix -> questionTermsSet.intersect(answerTerms.toSet).size.toFloat / questionLength,
        "LongestMatchSpan" + suffix -> longestSpan
      )
    } else {
      Map[String, Float]()
    }
  }

  /**
   * Compute a set of features for the given answer candidate.
   * @param question Current question, for which the candidate was generated.
   * @param answer Answer candidate to compute features for.
   * @return A map from feature names to the corresponding values.
   */
  override def computeFeatures(question: Question, answer: AnswerCandidate): Map[String, Float] = {
    val titleTerms = NlpUtils.getLemmas(question.titleNlp)
    val bodyTerms = NlpUtils.getLemmas(question.bodyNlp)
    val answerTerms = NlpUtils.getLemmas(answer.textNlp)

    val answerQTitle = answer.getAttributeNlp(QuestionTitle)
    val answerQTitleTerms = if (answerQTitle.isDefined) NlpUtils.getLemmas(answerQTitle.get) else Nil
    val answerQBody = answer.getAttributeNlp(QuestionBody)
    val answerQBodyTerms = if (answerQBody.isDefined) NlpUtils.getLemmas(answerQBody.get) else Nil

    getTermMatchFeatures(titleTerms, answerTerms, "Title-Answer") ++
    getTermMatchFeatures(bodyTerms, answerTerms, "Body-Answer") ++
    getTermMatchFeatures(titleTerms, answerQTitleTerms, "Title-AnswerQTitle") ++
    getTermMatchFeatures(titleTerms, answerQBodyTerms, "Title-AnswerQBody") ++
    getTermMatchFeatures(bodyTerms, answerQTitleTerms, "Body-AnswerQTitle") ++
    getTermMatchFeatures(bodyTerms, answerQBodyTerms, "Body-AnswerQBody")
  }
}
