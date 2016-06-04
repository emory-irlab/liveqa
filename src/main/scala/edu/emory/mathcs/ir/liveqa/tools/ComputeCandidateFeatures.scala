package edu.emory.mathcs.ir.liveqa.tools

import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate
import edu.emory.mathcs.ir.liveqa.scoring.features._

import collection.JavaConverters._

import org.apache.commons.csv.{CSVFormat, CSVParser}

/**
 * Created by dsavenk on 6/3/16.
 */
object ComputeCandidateFeatures {

  def main(args: Array[String]): Unit = {
    val inputFile = scala.io.Source.fromFile(args(0))
    val csv = CSVParser.parse(args(0), CSVFormat.DEFAULT)

    val featureCalculator = new MergeFeatures(
      new Bm25Features, new AnswerStatsFeatures, new TermOverlapFeatures, new MatchesFeatures, new SourceFeatures
    )

    val answers = csv.iterator().asScala.flatMap(r => (1 to 9).map(i => (r.get("Input.answer_) r.get("Input.answer_" + i)))).map { text =>
      new AnswerCandidate(type, string, source)
    }
    println(answers.length)
  }
}
