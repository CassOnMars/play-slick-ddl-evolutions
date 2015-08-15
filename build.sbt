name := """play-slick-ddl-evolutions"""

version := "1.0"

scalaVersion := "2.11.7"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  ("org.reflections" % "reflections" % "0.9.8" notTransitive())
      .exclude("com.google.guava", "guava") //provided by play
      .exclude("javassist", "javassist")
)

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"

