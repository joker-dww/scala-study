name := """play-slick-quickstart"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6" // or "2.10.4"

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.25",
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "com.typesafe.play" %% "play-slick" % "0.8.1",
  "org.springframework" % "spring-beans" % "3.2.3.RELEASE"
)

fork in Test := false

lazy val root = (project in file(".")).enablePlugins(PlayScala)