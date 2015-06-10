name := """play-slick-quickstart"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6" // or "2.10.4"

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.25",
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "com.typesafe.play" %% "play-slick" % "0.8.1",
  "org.springframework" % "spring-core" % "3.2.3.RELEASE",
  "org.springframework" % "spring-beans" % "3.2.3.RELEASE",
  "org.springframework" % "spring-context" % "3.2.3.RELEASE",
  "org.springframework" % "spring-context-support" % "3.2.3.RELEASE",
  "org.springframework" % "spring-aop" % "3.2.3.RELEASE",
  "org.springframework" % "spring-aspects" % "3.2.3.RELEASE",
  "org.springframework" % "spring-orm" % "3.2.3.RELEASE",
  "org.springframework" % "spring-webmvc" % "3.2.3.RELEASE",
  "org.springframework.mobile" % "spring-mobile-device" % "1.0.0.RELEASE",
  "org.springframework.security" % "spring-security-core" % "3.1.4.RELEASE",
  "org.springframework.security" % "spring-security-config" % "3.1.4.RELEASE",
  "org.springframework.security" % "spring-security-web" % "3.1.4.RELEASE",
  "org.springframework.data" % "spring-data-jpa" % "1.0.2.RELEASE",
  "org.springframework.data" % "spring-data-redis" % "1.0.0.RELEASE",
  "org.apache.mahout" % "mahout-core" % "0.9",
  "org.apache.mahout" % "mahout-math" % "0.9",
  "org.apache.mahout" % "mahout-integration" % "0.9"
)

fork in Test := false

lazy val root = (project in file(".")).enablePlugins(PlayScala)