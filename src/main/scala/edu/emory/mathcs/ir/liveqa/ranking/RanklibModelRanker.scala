package edu.emory.mathcs.ir.liveqa.ranking

import ciir.umass.edu.learning.{DataPoint, Ranker, RankerFactory}
import edu.emory.mathcs.ir.liveqa.base.AnswerCandidate.{CandidateAttribute, CandidateScore}
import edu.emory.mathcs.ir.liveqa.base.{AnswerCandidate, Question}
import edu.emory.mathcs.ir.liveqa.ranking.ranklib.Converter
import edu.emory.mathcs.ir.liveqa.scoring.features.FeatureCalculation

import scala.pickling.Defaults._
import scala.pickling.binary._

/**
  * Created by dsavenk on 5/27/16.
  */
class RanklibModelRanker(ranker: Ranker,
                         featureGenerator: FeatureCalculation,
                         alphabet: collection.mutable.Map[String, Int]) extends AnswerRanking {

  /**
    * Ranks a set of candidate answers according to some criteria.
    *
    * @param question   Question to which to rank the candidates.
    * @param candidates Answer candidates to rank.
    * @return A ranked list of candidate answers.
    */
  override def rank(question: Question, candidates: Seq[AnswerCandidate]): Seq[AnswerCandidate] = {
    if (candidates.isEmpty) candidates
    else {
      candidates.foreach(c => featureGenerator.computeFeatures(question, c).map(e => c.features += e))

      val ranklist = Converter.createRankList(question, candidates, alphabet)
      val candidateWithScores = (0 until ranklist.size)
        .map(ranklist.get)
        .map(ranker.eval)
        .zip(candidates).sortBy {
        case (score, candidate) => -score
      }

      candidateWithScores.map {
        case (score, candidate) =>
          candidate.attributes(CandidateScore) = score.toString
          candidate
      }
    }
  }
}

object RanklibModelRanker {
  object RankLibModelRank extends CandidateAttribute

  def create(rankerFile: String, featureGenerator: FeatureCalculation, alphabetFile: String): RanklibModelRanker = {
    val model = new RankerFactory().loadRankerFromFile(rankerFile)
    val rawData = BinaryPickle(scala.io.Source.fromFile(alphabetFile).map(_.toByte).toArray)
    val alphabet = rawData.unpickle[collection.mutable.Map[String, Int]]

    new RanklibModelRanker(model, featureGenerator, alphabet)
  }
}