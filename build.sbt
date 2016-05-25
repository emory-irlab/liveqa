name := "liveqa"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "twttr" at "https://maven.twttr.com/"
resolvers += "jboss" at "http://repository.jboss.org/nexus/content/groups/public-jboss/"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
  "ch.qos.logback" %  "logback-classic" % "1.1.7",
  "com.github.finagle" %% "finch-core" % "0.10.0",
  "com.github.finagle" %% "finch-circe" % "0.10.0",
  "com.google.protobuf" % "protobuf-java" % "2.6.1",
  "com.twitter" %% "twitter-server" % "1.20.0",
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models",
  "io.circe" %% "circe-generic" % "0.3.0",
  "log4j" % "log4j" % "1.2.17",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
  "net.ettinsmoor" %% "bingerator" % "0.2.4",
  "net.ettinsmoor" % "java-aws-mturk" % "1.6.2",
  "net.ruippeixotog" %% "scala-scraper" % "1.0.0",
  "org.apache.tika" % "tika-parsers" % "1.12",
  "org.joda" % "joda-convert" % "1.7",
  "org.xerial" % "sqlite-jdbc" % "3.8.11.2"
)