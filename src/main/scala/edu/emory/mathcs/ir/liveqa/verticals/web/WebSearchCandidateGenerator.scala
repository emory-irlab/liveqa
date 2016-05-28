package edu.emory.mathcs.ir.liveqa.verticals.web

import com.twitter.util.Future
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.{CandidateSourceRank, QuestionBody, QuestionTitle}
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, CandidateGeneration, QueryGeneration, Question}

/**
  * Generates candidate answers using web search to retrieve a set of relevant
  * documents, parse them and select useful passages along with some helpful
  * information.
  */
class WebSearchCandidateGenerator
      extends CandidateGeneration with QueryGeneration {

  /**
    * Generates web search queries for the given question.
    * @param question A [[Question]] instance to generate search queries for.
    * @return A [[Seq]] of search queries that can be issued to a search engine.
    */
  override def getSearchQueries(question: Question) = {
    Seq(question.title)
  }

  /**
    * Generates candidates from a single web document.
    * @param document [[WebDocument]] to generate answer candidates from.
    * @return A future sequence of candidate answers.
    */
  def generateCandidates(document: WebDocument, rank: Int): Future[Seq[AnswerCandidate]] = {
    val candidatesFuture = document.content.map {
      docContent =>
        if (docContent.isDefined)
          // TODO(denxx): Generate better candidates from the content.
          Seq(new AnswerCandidate(AnswerCandidate.WEB, document.description, document.url)) ++
          ContentExtractor(docContent.get).map(new AnswerCandidate(AnswerCandidate.WEB, _, document.url))
        else
          Nil
    }

    var prevCandidate: String = ""
    candidatesFuture.map(candidates => candidates.foreach {
      c =>
        c.attributes(QuestionTitle) = document.title
        c.attributes(QuestionBody) = prevCandidate
        c.attributes(CandidateSourceRank) = rank.toString
        prevCandidate = c.text
    })

    candidatesFuture
  }

  /**
    * Generates candidate answers using web search to retrieve documents.
    *
    * @param question A question to generate the answer to.
    * @return A future sequence of candidate answers.
    */
  override def getCandidateAnswers(question: Question)
      : Future[Seq[AnswerCandidate]] = {
    val queries = getSearchQueries(question)
    Future.collect(queries.map {
      query =>
        Future.collect(WebSearch(query).zipWithIndex.map(d => generateCandidates(d._1, d._2)))
          .map(_.flatten)  // We first collect futures, and then flatten nested
                           // sequences.
    }).map(_.flatten)
  }
}
