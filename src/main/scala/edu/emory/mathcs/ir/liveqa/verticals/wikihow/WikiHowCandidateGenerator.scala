package edu.emory.mathcs.ir.liveqa.verticals.wikihow

/**
  * Created by dsavenk on 5/28/16.
  */
class WikiHowCandidateGenerator {

}

////get article pages from a search results page on WikiHow
////Note: article pages can be navigated to by adding "&start=number" to the end of
////the URL. If the number is 10, results 11-20 will be displayed.
//private def parseSearchResultsPageWikiHow(pageHtml: String): WikiHowQuestion = {
//  val browser = JsoupBrowser()
//  val document = browser.parseString(pageHtml)
//
//  //get the number of results in order to guide search of other results pages
//  val resultsText = document >> text("div[class=sr_foot_results]")
//  //The text will be in the following form:
//  //Results 11-20 of 179,000
//  //The last number is the total number of results
//
//  //get the article URLs
//  val resultList = document >> element("#searchresults_list")
//  val articleURLs = resultList >> attr("href")("a .result_link")
//  val articleTitles = resultList >> text("a .result_link")
//}
//
//  //get the relevant text, the title, and the text of an article on WikiHow
//  private def parseArticle(pageHtml: String): WikiHowQuestion = {
//  val browser = JsoupBrowser()
//  val document = browser.parseString(pageHtml)
//  //val answer
//
//  //get intro text
//  val intro = document >> element("#intro")
//  val title = intro >> text("h1[class=firstHeading]")
//  //add the following to answer: intro > texts("p[class=None]")
//
//  //get section text
//  //add the following to answer: document >> texts("div[class=section steps sticky ] div[class=section_text] div[class=step]")
//
//  //get tip text(if it's there)
//  val tipText = document >?> texts("div[class=sectiion tips sticky ] div[class=section_text]")
//  //add tipText to answer
//
//  //get warning text(if it's there)
//  val warningText = document >?> texts("div[class=section warnings sticky ] div[class=section_text]")
//  //add warningText to answer
//
//  //get things you'l need text(if it's there)
//  val thingsNeededText = document >?> texts("div[class=section thingsyoullneed sticky ] div[class=section_text]")
//  //add thingsNeededText to answer
//
//  //get the category
//  val thirdLi = elements("#breadcrumb li")/*[3]*/
//  val category = text(thirdLi)
//}