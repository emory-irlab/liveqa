package edu.emory.mathcs.ir.liveqa.verticals.webmd

/**
  * Created by dsavenk on 5/28/16.
  */
class WebMdCandidateGenerator {

}

////get answer pages from the HTML of a search results page on WebMD
////Note: Other answer pages can be navigated to by adding "&pagenumber=desired page number" to the URL.
////If the URL already has "&pagenumber=some number" at the end of it, change the number
//private def parseSearchResultsPageWebMD(pageHtml: String): WebMDQuestion = {
//  val browser = JsoupBrowser()
//  val document = browser.parseString(pageHtml)
//  //val articleURLs
//  //val articleTitles
//  //val previewTexts
//
//  //gets the total number of results pages
//  val pageLinks = document >> elementList(".pages a")
//  //val numberOfResultsPages = text("the last element in pageLinks") //strip of spaces first
//
//  //gets spotlight article pages from the search results page
//  val spotlights = document >> elementList("div .spotlight_results")
//  //for <item in spotlights>:
//  val relevantDiv = item >> elementList(".text_fmt") //this list should only have 1 item
//  val title = item >> text("h2")
//  val articleURL = item >> attr("href")("h2 a")
//  val previewText = item >> text(".description_fmt")
//  //add title to titles, articleURL to articleURLs, and previewText to previewTexts
//
//  //gets non-spotlight article pages from the search results page
//  val nonspotlights = document >> elementList("ul .searchResults_fmt li")
//  //for <item in nonspotlights>:
//  val title = item >> text("a")
//  val articleURL = item >> attr("href")("a")
//  val previewText = item >> text("div .searchDescription_fmt")
//  //add title to titles, articleURL to articleURLs, and previewText to previewTexts
//
//  //Return the articleURLs, articleTitles, and previewTexts lists?
//}
//
//  //get answer text from an article's HTML on WebMD
//  //Note: Other pages in the article can be navigated to by adding "?page=desired page number" to the URL
//  //if the URL already ends in "?page=some number", change the number
//  private def parseArticleWebMD(pageHtml: String): WebMDQuestion = {
//  val browser = JsoupBrowser()
//  val document = browser.parseString(pageHtml)
//
//  //get the number of pages in the article    //useful for extracting a multi-page answer
//  val nextPageLinks = document >> elementList(".right_fmt a")
//  //val articlePageNumber = text("the last element in nextPageLinks")
//
//  val textArea = document >> element("#textArea") //is this how you get a single element?
//  val title = textArea >> text("h2")//needs to be the text of the first h2
//  val answer = textArea >> texts("li, h3, h4, p, dt, dd") //needs to be condensed into one variable
//
//  //return title and answer?
//}