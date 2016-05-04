package edu.emory.mathcs.ir.liveqa.base

/**
  * Created by dsavenk on 4/26/16.
  */
trait QueryGeneration {
  /**
    * Generate a [[Seq]] of search queries for the given question.
    * @param question A [[Question]] instance to generate search queries for.
    * @return A [[Seq]] of search queries that can be issued to a search engine.
    */
  def getSearchQueries(question: Question): Seq[String]
}
