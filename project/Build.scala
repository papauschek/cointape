
import com.typesafe.sbt.jse.JsEngineImport.JsEngineKeys
import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.gzip.Import._
import com.typesafe.sbt.rjs.Import._
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.SbtWeb
import play.Play.autoImport._
import sbt.Keys._
import sbt._

object ApplicationBuild extends Build {

  override def settings = super.settings ++ Seq(
    scalaVersion := "2.11.6",
    version := "0.1"
  )

  val commonSettings = Seq(
    sources in doc in Compile := List(), // skip api generation
    scalacOptions ++= Seq("-feature"),
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-ws" % "2.3.8",
      "org.logback-extensions" % "logback-ext-loggly" % "0.1.2"
    )
  )

  /** codegen project containing customized DB code generator */
  lazy val dbGen = Project(
    id="dbgen",
    base=file("dbgen"),
    settings = Seq(
      libraryDependencies ++= List(
        "com.typesafe.slick" %% "slick-codegen" % "3.0.0",
        "org.postgresql" % "postgresql" % "9.4-1200-jdbc41"
      ),
      libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-simple")) }
    )
  )

  /** data model */
  lazy val model = Project(
    "cointape-model", file("model"), settings = commonSettings).settings(
    resolvers += Resolver.url("Edulify Repository", url("http://edulify.github.io/modules/releases/"))(Resolver.ivyStylePatterns),
    libraryDependencies ++= Seq(
      "org.joda" % "joda-convert" % "1.7",
      "joda-time" % "joda-time" % "2.7",
      "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0",
      "com.typesafe.slick" %% "slick" % "3.0.0",
      "com.zaxxer" % "HikariCP-java6" % "2.3.7",
      "com.edulify" %% "play-hikaricp" % "2.0.5"
    )
  ).dependsOn(dbGen)

  /** play website */
  lazy val main = Project("cointape-main", file("."), settings = commonSettings).
    enablePlugins(play.PlayScala, SbtWeb).
    settings(
      JsEngineKeys.engineType := JsEngineKeys.EngineType.Node,
      slick <<= slickCodeGenTask, // register manual sbt command
      libraryDependencies ++= Seq(
        jdbc,
        cache, // play cache external module
        ws
      ),

      RjsKeys.mainModule := "app",
      pipelineStages := Seq(rjs, digest, gzip)

  ).dependsOn(model)

  /** command line project */
  lazy val tools = Project(
    "cointape-tools", file("tools"), settings = commonSettings).settings(
  ).dependsOn(model)

  // code generation task
  lazy val slick = TaskKey[Seq[File]]("gen-tables")
  lazy val slickCodeGenTask = (sourceManaged, dependencyClasspath in Compile, runner in Compile, streams) map { (dir, cp, r, s) =>
    //val outputDir = (dir / "slick").getPath // place generated files in sbt's managed sources folder
    val outputDir = (dir / ".." / ".." / ".." / "model" / "src" / "main" / "scala").getPath // place generated files in sbt's managed sources folder
    val url = "jdbc:postgresql://localhost/cointape"
    val jdbcDriver = "org.postgresql.Driver"
    val slickDriver = "slick.driver.PostgresDriver"
    val pkg = "db"
    val user = "cointape"
    val password = "cointape"
    toError(r.run("DbCodeGenerator", cp.files, Array(slickDriver, jdbcDriver, url, outputDir, pkg, user, password), s.log))
    val fname = outputDir + "/db/Tables.scala"
    Seq(file(fname))
  }

}
