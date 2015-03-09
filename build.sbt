// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "linked-data-explorer"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.5"

persistLauncher in Compile := true

persistLauncher in Test := false

testFrameworks += new TestFramework("utest.runner.Framework")

val bananaVersion = "0.8.0.20150307"

resolvers += Resolver.url("bertails.org snapshots", new java.net.URL("http://bertails.org/repo/"))(Resolver.ivyStylePatterns)

resolvers += Resolver.url("inthenow-releases", url("http://dl.bintray.com/inthenow/releases"))(Resolver.ivyStylePatterns)

//resolvers += bintray.Opts.resolver.repo("denigma", "denigma-releases")
resolvers += "denigma-releases" at "http://dl.bintray.com/denigma/denigma-releases/"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.8.0",
  "com.lihaoyi" %%% "utest" % "0.3.0" % "test",
  "org.w3" %%% "banana-plantain" % bananaVersion,
  "org.w3" %%% "banana-n3-js" % bananaVersion,
  "org.w3" %%% "banana-jsonld-js" % bananaVersion,
  "org.scalajs" %%% "threejs" % "0.0.68-0.1.4"
)

skip in packageJSDependencies := false
