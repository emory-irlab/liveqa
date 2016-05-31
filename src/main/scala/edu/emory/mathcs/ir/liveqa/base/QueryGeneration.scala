package edu.emory.mathcs.ir.liveqa.base

import collection.JavaConverters._
import edu.emory.mathcs.ir.liveqa.util.{NlpUtils, Stopwords, TermIdf}

/**
  * Created by dsavenk on 4/26/16.
  */
trait QueryGeneration {
  /**
    * Generate a [[Seq]] of search queries for the given question.
    *
    * @param question A [[Question]] instance to generate search queries for.
    * @return A [[Seq]] of search queries that can be issued to a search engine.
    */
  def getSearchQueries(question: Question): Seq[String]
}

class CombineQueriesGenerator(queryGenerators: QueryGeneration*) extends QueryGeneration {
  /**
    * Generate a [[Seq]] of search queries for the given question.
    *
    * @param question A [[Question]] instance to generate search queries for.
    * @return A [[Seq]] of search queries that can be issued to a search engine.
    */
  override def getSearchQueries(question: Question): Seq[String] = {
    queryGenerators.flatMap(_.getSearchQueries(question))
  }
}

class TitleQueryGeneration extends QueryGeneration {
  /**
    * Generate a [[Seq]] of search queries for the given question.
    *
    * @param question A [[Question]] instance to generate search queries for.
    * @return A [[Seq]] of search queries that can be issued to a search engine.
    */
  override def getSearchQueries(question: Question): Seq[String] = {
    Seq(question.title.replaceAll("[^A-Za-z0-9 ]", " "))
  }
}

class Top5IdfQueryGenerator(k: Int = 5) extends QueryGeneration {
  /**
    * Generate a [[Seq]] of search queries for the given question.
    *
    * @param question A [[Question]] instance to generate search queries for.
    * @return A [[Seq]] of search queries that can be issued to a search engine.
    */
  override def getSearchQueries(question: Question): Seq[String] = {
    val terms = NlpUtils.getLemmas(question.titleNlp)
      .filter(Stopwords.not)
      .sortBy(TermIdf(_)).reverse.take(k).toSet

    val query = Seq(question.titleNlp.sentences.asScala.flatMap { s =>
      (0 until s.length())
        .filter(i => s.posTag(i).startsWith("W") ||
          s.posTag(i).startsWith("V") ||
          s.nerTag(i) != "O" ||
          terms.contains(s.lemma(i).toLowerCase))
        .map(i => s.lemma(i))
    }.mkString(" "))

    query
  }
}

class LongestQuestionQueryGenerator extends QueryGeneration {
  /**
    * Generate a [[Seq]] of search queries for the given question.
    *
    * @param question A [[Question]] instance to generate search queries for.
    * @return A [[Seq]] of search queries that can be issued to a search engine.
    */
  override def getSearchQueries(question: Question): Seq[String] = {
    (question.titleNlp.sentences().asScala.toList :::
      question.bodyNlp.sentences().asScala.toList)
      .filter(s => s.posTag(s.length() - 1) == "?")
      .map(_.text)
      .sortBy(_.length).reverse.take(2)
  }
}