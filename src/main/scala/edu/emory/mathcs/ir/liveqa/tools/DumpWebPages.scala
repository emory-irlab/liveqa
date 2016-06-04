package edu.emory.mathcs.ir.liveqa.tools

import java.io.{File, PrintWriter}

import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.Relevance
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * Created by dsavenk on 5/29/16.
 */
object DumpWebPages extends App {
  val src = scala.io.Source.fromFile(args(0))
  val out = new PrintWriter(new File(args(1)))
  for (line <- src.getLines()) {
    line.split("\t", -1) match {
      case Array(_, qid, rel, title, body, cat, mainCat) if rel.isEmpty => None
      case Array(_, qid, rel, text, source, _, _) =>
        if (source.startsWith("http"))
          out.println(source.split(";")(0))
        else None
      case _ => None
    }
  }
  out.close()
}
