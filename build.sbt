// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "linked-data-explorer"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.5"

persistLauncher in Compile := true

persistLauncher in Test := false

testFrameworks += new TestFramework("utest.runner.Framework")

val bananaVersion = "0.8.0.93d16e0"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.8.0",
  "com.lihaoyi" %%% "utest" % "0.3.0" % "test",
  "org.w3" %%% "banana-plantain" % bananaVersion,
  "org.w3" %%% "banana-n3-js" % bananaVersion,
  "org.w3" %%% "banana-jsonld-js" % bananaVersion
)

skip in packageJSDependencies := false
