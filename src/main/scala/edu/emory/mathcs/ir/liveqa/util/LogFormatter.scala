package edu.emory.mathcs.ir.liveqa.util

/**
  * Formats a message to be sent to the log.
  */
object LogFormatter {
  def apply(eventType: String, fields: Array[String]) : String = {
    (eventType :: fields.map(
      field => field.replace("\t", " ").replace("\n", " ").replace("\r", ""))
      .toList)
      .mkString("\t")
  }
}
