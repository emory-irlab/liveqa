package edu.emory.mathcs.ir.liveqa.verticals.answerscom

import com.twitter.util.Future
import com.typesafe.config.ConfigFactory
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.ANSWERS_COM
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, CandidateGeneration, QueryGeneration, Question}

/**
  * An object that generates candidates answers for the given question by
  * retrieving related questions using Yahoo! Answers search functionality.
  */
class AnswersComCandidateGenerator
  extends CandidateGeneration with QueryGeneration {
  private val cfg = ConfigFactory.load()

  /**
    * Generates search queries for the given question.
    * @param question A question to generate search queries for.
    * @return A sequence of search queries to issue to a search engine.
    */
  override def getSearchQueries(question: Question) = {
    Seq(question.title.replaceAll("[^A-Za-z0-9]", " "))
  }

  override def getCandidateAnswers(question: Question)
      : Future[Seq[AnswerCandidate]] = {
    val results = Future collect {
      getSearchQueries(question) map {
        Search(_, cfg.getInt("qa.answers_com_results"))
      } map {
        futureResults => futureResults.map(
          results => results.flatten.flatMap(createCandidates(_)))
      }
    }
    results.map(futureResults => futureResults.flatten)
  }

  private def createCandidates(questionAnswers: AnswersComQuestion)
  :Seq[AnswerCandidate] = {
    questionAnswers.answers
      .zipWithIndex
      .map {
        case (answer: String, rank: Int) =>
          val candidate = new AnswerCandidate(
            ANSWERS_COM, answer, questionAnswers.url)

          // Add question attributes, so we could use them later.
          candidate.attributes(AnswerCandidate.CandidateSourceRank) = rank.toString
          candidate.attributes(AnswerCandidate.QuestionTitle) =
            questionAnswers.title
          candidate.attributes(AnswerCandidate.QuestionBody) =
            questionAnswers.body
          candidate.attributes(AnswerCandidate.QuestionMainCategory) =
            questionAnswers.categories.headOption.getOrElse("")
          candidate.attributes(AnswerCandidate.QuestionCategories) =
            questionAnswers.categories.mkString("\t")
          candidate.attributes(AnswerCandidate.QuestionCategories) =
            questionAnswers.qid

          // Return the answer.
          candidate
      }
  }
}

////get answer pages from the HTML of a search results page on WikiAnswers
////Note: a search query is made using the following URL:
////www.answers.com/search?q=encoded+question+text
//private def parseSearchResultsPageWikiAnswers(pageHtml: String): WikiAnswersQuestion = {
//  val browser = JsoupBrowser()
//  val document = browser.parseString(pageHtml)
//  //val articleURLs
//  //val questions
//  //val previewTexts
//
//  val articles = document >> elementList("article")
//  //val answerPages
//  //for <article in articles>:
//  //	if <attr("class")(article) == "frame">:
//  //		if <article >?> elementList(".category_name") not None>:
//  //it's an answer page
//  //			val question = article >> text(".content .title a") //first element
//  //			val articleURL = article >> attr("href")(".content .title a") //first element
//  //			val previewText = article >> text(".content .content_text") //remove "(MORE)" from the end if it's there
//  //			//add question to questions, articleURL to articleURLs, and previewText to previewTexts
//
//  //return the articleURLs, articleTitles, and previewTexts lists?
//}
//
//  //get the question, the category of the question, the and the answers from an answer page on WikiAnswers
//  private def parseAnswerWikiAnswers(pageHtml: String): WikiAnswersQuestion = {
//  val browse = JsoupBrowser()
//  val document = browser.parseString(pageHtml)
//
//  val answerArea = document >?> element(".module utility_wrapper_card jsparams js-utility_wrapper_card no_picture")
//  //if answerArea == None:
//  //	val answerArea = document >> element(".module utility_wrapper_card jsparams js-utility_wrapper_card picture")
//
//  val category = answerArea >> text(".category_name")
//  val question = answerArea >> text(".title_edit_wrapper .title_text")
//  val answers = answerArea >> texts(".answer_wrapper .answer_text")
//
//  //return category, question, and answers lists?
//}