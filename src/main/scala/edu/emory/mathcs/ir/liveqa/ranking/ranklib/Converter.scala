package edu.emory.mathcs.ir.liveqa.ranking.ranklib

import collection.JavaConverters._
import ciir.umass.edu.learning.{DataPoint, RankList}
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.Relevance
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}

/**
  * Created by dsavenk on 5/27/16.
  */
object Converter {
  def createDataset(instances: Seq[(Question, Seq[AnswerCandidate])], alphabet: collection.mutable.Map[String, Int]): List[RankList] = {
    instances.map {
      case (question, candidates) => createRankList(question, candidates, alphabet)
    }.toList
  }

  def createRankList(question: Question, candidates: Seq[AnswerCandidate], alphabet: collection.mutable.Map[String, Int]): RankList = {
    new RankList(candidates.zipWithIndex.map(c => createDataPoint(question, c._1, c._2.toString, alphabet)).toList.asJava)
  }

  def createDataPoint(question: Question, candidate: AnswerCandidate,
                      candidateId: String, alphabet: collection.mutable.Map[String, Int]): DataPoint = {
    val label = math.max(0, candidate.attributes(Relevance).toFloat - 1)
    val point = new AlphabetSparseDataPoint(label, question.qid, candidate.features.toMap, alphabet)
    point.setDescription(candidateId)
    point
  }
}
