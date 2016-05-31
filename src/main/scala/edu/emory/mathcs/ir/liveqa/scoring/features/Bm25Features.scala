package edu.emory.mathcs.ir.liveqa.scoring.features

import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.{QuestionBody, QuestionTitle}

import collection.JavaConverters._
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import edu.emory.mathcs.ir.liveqa.util.{Stopwords, TermIdf}
import edu.stanford.nlp.simple.{Document, Sentence}

/**
  * Computes BM25 features for the given question - answer pair.
  */
class Bm25Features extends FeatureCalculation {
  /**
    * Compute a set of features for the given answer candidate.
    *
    * @param question Current question, for which the candidate was generated.
    * @param answer   Answer candidate to compute features for.
    * @return A map from feature names to the corresponding values.
    */
  override def computeFeatures(question: Question, answer: AnswerCandidate): Map[String, Float] = {
    val titleAndBody = question.titleNlp.sentences.asScala.toList :::
                       question.bodyNlp.sentences.asScala.toList
    val answerQTitleSentences =
      answer.getAttributeNlp(QuestionTitle)
        .map(d => d.sentences.asScala.toList).getOrElse(Nil)
    val answerQBodySentences =
      answer.getAttributeNlp(QuestionBody)
        .map(d => d.sentences.asScala.toList).getOrElse(Nil)

    val res = Map(
      "BM25Title-Answer" -> BM25Score(question.titleNlp, answer.textNlp),
      "BM25Body-Answer" -> BM25Score(question.bodyNlp, answer.textNlp),
      "BM25Full-Answer" -> BM25Score(titleAndBody, answer.textNlp.sentences.asScala),
      "BM25Title-AnswerQTitle" -> BM25Score(question.titleNlp.sentences.asScala, answerQTitleSentences),
      "BM25Title-AnswerQBody" -> BM25Score(question.titleNlp.sentences.asScala, answerQBodySentences),
      "BM25Title-AnswerQTitleBody" -> BM25Score(question.titleNlp.sentences.asScala, answerQTitleSentences ::: answerQBodySentences),
      "BM25Full-AnswerQTitleBody" -> BM25Score(titleAndBody, answerQTitleSentences ::: answerQBodySentences)
    )
    res
  }


}

/**
  * Returns BM25 score for a pair of documents.
  */
object BM25Score {
  val defaultK1 = 1.2f
  val defaultB = 0.75f
  val defaultAvgL = 100f

  def apply(query: Document,
            document: Document): Float =
    apply(query.sentences.asScala, document.sentences.asScala)

  def apply(query: Seq[Sentence],
            document: Seq[Sentence],
            k1: Float = defaultK1,
            b: Float = defaultB,
            avgL: Float = defaultAvgL) : Float = {
    val queryTerms = query.flatMap(s => s.lemmas().asScala
      .map(l => l.toLowerCase())
      .filter(l => l.headOption.getOrElse(' ').isLetterOrDigit &&
        Stopwords.not(l))).toSet
    val docTerms = document.flatMap(s => s.lemmas.asScala)
      .map(l => l.toLowerCase())
      .filter(l => l.headOption.getOrElse(' ').isLetterOrDigit &&
        Stopwords.not(l))
    val documentTermsCount = docTerms
      .filter(queryTerms.contains)
      .groupBy(l => l).map {
        case (k, v) => (k, v.size)
      }

    val bm25 = documentTermsCount.map { case (term, df) =>
      val denominator = df + k1 * (1 - b + b * (docTerms.size / avgL))
      TermIdf(term) * df * (k1 + 1) / denominator
    }.sum

    bm25
  }
}