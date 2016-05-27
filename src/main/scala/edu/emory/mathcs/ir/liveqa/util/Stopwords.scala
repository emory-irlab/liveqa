package edu.emory.mathcs.ir.liveqa.util

import java.io.InputStream

/**
  * Created by dsavenk on 5/27/16.
  */
object Stopwords {
  val stream: InputStream = getClass.getResourceAsStream("/data/stopwords.txt")
  val stopwords = scala.io.Source.fromInputStream( stream ).getLines.toSet

  def has(word: String): Boolean = stopwords.contains(word.toLowerCase)

  def not(word: String): Boolean = !has(word)
}
