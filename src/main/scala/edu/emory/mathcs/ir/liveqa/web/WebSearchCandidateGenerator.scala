package edu.emory.mathcs.ir.liveqa.web

import com.twitter.util.Future
import edu.emory.mathcs.ir.liveqa.base.{Question, QueryGeneration, CandidateGeneration, AnswerCandidate}

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
  def generateCandidates(document: WebDocument): Future[Seq[AnswerCandidate]] = {
    document.content.map {
      docContent =>
        if (docContent.isDefined)
          // TODO(denxx): Generate better candidates from the content.
          Seq(new AnswerCandidate(AnswerCandidate.WEB, document.description, document.url))
        else
          Nil
    }
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
        Future.collect(WebSearch(query).map(generateCandidates(_)))
          .map(_.flatten)  // We first collect futures, and then flatten nested
                           // sequences.
    }).map(_.flatten)
  }
}
