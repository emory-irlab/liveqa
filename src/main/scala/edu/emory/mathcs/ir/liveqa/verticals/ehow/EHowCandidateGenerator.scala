package edu.emory.mathcs.ir.liveqa.verticals.ehow

/**
  * Created by dsavenk on 5/28/16.
  */
class EHowCandidateGenerator {

}

////get answer pages from a search results page on eHow
////Note: Other answer pages can be navigated to by adding "&p=desired page number" to the URL.
////If the URL already has "&p=some number" at the end of it, change the number
//private def parseSearchResultsPageEHow(pageHtml: String): EHowQuestion = {
//  val browser = JsoupBrowser()
//  val document = browser.parseString(pageHtml)
//  //val articleURLs
//  //val articleTitles
//  //val previewTexts
//
//  //get the highest page number visible from this page (Note: this may
//  //not be the total number of search results pages)
//  val pageLis = document >> elements(".pagination unstyled li")
//  val highestNumberVisible = text(the second-to-last element of pageLis)
//
//  //get all articles
//  val articles = document >> elements(".results unstyled mod li")
//  //for <article in articles>:
//  val as = articles >> elements("a")/*[0]*/
//  val articleURL = attr("href")(as)
//  val articleTitle = text(as)
//  val previewText = as >> text("p")
//  //add articleURL to articleURLs, articleTitle to articleTitles, previewText to PreviewTexts
//}
//
//  //get answer text from an article's HTML on eHow
//  private def parseArticleEHow(pageHtml: String): EHowQuestion = {
//  val browser = JsoupBrowser()
//  val document = browser.parseString(pageHtml)
//
//  val title = document >> text(".page-head h1")
//  val rels = document >> elements("a[rel=directory]")
//  //val category = text(first element in rels)
//
//  val article = elements("article[data-type=article]")/*[0]*/
//  val answerPt1 = article >> text("div[data-module=article-intro]")
//  val answerPt2 = article >> text("section[data-module=article-body] .adWrapper")
//  val answerPt3 = article >> text("section[data-module=article-body] span")
//}