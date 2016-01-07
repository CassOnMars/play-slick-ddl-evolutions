name := """play-slick-ddl-evolutions"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  ("org.reflections" % "reflections" % "0.9.8" notTransitive())
      .exclude("com.google.guava", "guava") //provided by play
      .exclude("javassist", "javassist"),
  "com.typesafe.play" %% "play-jdbc-api" % "2.4.2",
  "com.typesafe.play" %% "play-jdbc-evolutions" % "2.4.2"
)

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"

