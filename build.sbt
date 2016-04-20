name := "liveqa"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
  "com.github.finagle" %% "finch-core" % "0.10.0",
  "com.github.finagle" %% "finch-circe" % "0.10.0",
  "io.circe" %% "circe-generic" % "0.3.0",
  "net.ruippeixotog" %% "scala-scraper" % "1.0.0"
)