
// Comment to get more information during initialization
logLevel := Level.Info

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("releases")

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.8")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.7")
//addSbtPlugin("com.typesafe.sbt" % "sbt-uglify" % "1.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

//lazy val plugin = uri("git://github.com/mariussoutier/sbt-concat")

//lazy val root = (project in file(".")).dependsOn(plugin)

//addSbtPlugin("net.ground5hark.sbt" % "sbt-concat" % "0.1.8")

addSbtPlugin("com.github.ddispaltro" % "sbt-reactjs" % "0.5.0")
