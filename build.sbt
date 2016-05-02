name := "liveqa"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "twttr" at "https://maven.twttr.com/"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
  "ch.qos.logback" %  "logback-classic" % "1.1.7",
  "com.github.finagle" %% "finch-core" % "0.10.0",
  "com.github.finagle" %% "finch-circe" % "0.10.0",
  "com.twitter" %% "twitter-server" % "1.20.0",
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models",
  "io.circe" %% "circe-generic" % "0.3.0",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
  "net.ettinsmoor" %% "bingerator" % "0.2.4",
  "net.ruippeixotog" %% "scala-scraper" % "1.0.0",
  "org.apache.tika" % "tika-parsers" % "1.12"
)