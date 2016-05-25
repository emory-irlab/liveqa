package edu.emory.mathcs.ir.liveqa.crowd

import com.amazonaws.mturk.requester.QualificationRequirement
import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.mturk.util.PropertiesClientConfig
import com.typesafe.config.ConfigFactory

/**
  * Created by dsavenk on 5/20/16.
  */
object MturkTask {
  val cfg = ConfigFactory.load()

  val service = new RequesterService(
    new PropertiesClientConfig("src/main/resources/mturk.properties"))
  def balance = service.getAccountBalance

  val externalQuestion =
    <ExternalQuestion xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd">
      <ExternalURL>https://carbonite.mathcs.emory.edu/liveqa</ExternalURL>
      <FrameHeight>600</FrameHeight>
    </ExternalQuestion>

  def main(args: Array[String]): Unit = {

    val responseGroup: Array[String] = null
    val qualifications: Array[QualificationRequirement] = null
    val requesterAnnotation:String = null

    val hit = service.createHIT(null,
      cfg.getString("qa.crowd.hit.title"),
      cfg.getString("qa.crowd.hit.description"),
      cfg.getString("qa.crowd.hit.keywords"),
      externalQuestion.toString,
      cfg.getDouble("qa.crowd.hit.price"),
      16 * 60, // 16 minutes
      60 * 60 * 24 * 2, // 2 day auto approval
      cfg.getLong("qa.crowd.hit.expire_seconds"),
      cfg.getInt("qa.crowd.hit.count"),
      requesterAnnotation, //requesterAnnotation
      qualifications, // qualificationRequirements
      responseGroup)
  }
}
