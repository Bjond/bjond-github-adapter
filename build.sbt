name := """bjond-github-adapter"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
//EclipseKeys.preTasks := Seq(compile in Compile)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "org.reactivemongo" %% "reactivemongo" % "0.11.9"
)
unmanagedJars in Compile <<= baseDirectory map { base => (base ** "*.jar").classpath }

//libraryDependencies += "org.coursera" %% "autoschema" % "0.2"
libraryDependencies += "com.typesafe.play.extras" %% "iteratees-extras" % "1.5.0"
// libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"

// libraryDependencies += "org.bouncycastle" % "bcpkix-jdk15on"
libraryDependencies += "com.pauldijou" %% "jwt-play-json" % "0.7.1"
libraryDependencies += "org.bitbucket.b_c" % "jose4j" % "0.5.0"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
