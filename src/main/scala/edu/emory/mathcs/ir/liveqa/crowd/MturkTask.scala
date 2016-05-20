package edu.emory.mathcs.ir.liveqa.crowd

import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.mturk.util.PropertiesClientConfig
import com.typesafe.scalalogging.LazyLogging

/**
  * Created by dsavenk on 5/20/16.
  */
object MturkTask extends App with LazyLogging {
  val service = new RequesterService(
    new PropertiesClientConfig("src/main/resources/mturk.properties"))
  def balance = service.getAccountBalance

  val externalQuestion =
    <ExternalQuestion xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd">
      <ExternalURL>https://carbonite.mathcs.emory.edu/liveqa</ExternalURL>
      <FrameHeight>600</FrameHeight>
    </ExternalQuestion>

  service.createHIT("Answer questions in real time", "You will need to sit and wait for questions", 0.01, externalQuestion.toString, 1)
}
