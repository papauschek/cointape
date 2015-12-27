import java.net.URI

import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.{model => m}
import slick.codegen.SourceCodeGenerator

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Await}
import slick.util.ConfigExtensionMethods.configExtensionMethods

class DbCodeGenerator(model: m.Model) extends SourceCodeGenerator(model) {

  // add some custom imports
  // TODO: fix these imports to refer to your JdbcSupport and your Joda imports
  override def code = "import com.github.tototoshi.slick.PostgresJodaSupport._\n" + "import org.joda.time.DateTime\n" + super.code

  override def Table = new Table(_) {
    override def Column = new Column(_) {

      // munge rawType -> SQL column type HERE (scaladoc in Slick 2.1.0 is outdated or incorrect, GeneratorHelpers#mapJdbcTypeString does not exist)
      // you can filter on model.name for the column name or model.tpe for the column type
      // your IDE won't like the String here but don't worry, the return type the compiler expects here is String
      override def rawType = model.tpe match {
        case "java.sql.Timestamp"               => "DateTime" // kill j.s.Timestamp
        case _ => {
          //          println(s"${model.table.table}#${model.name} tpe=${model.tpe} rawType=${super.rawType}")
          super.rawType
        }
      }
    }
  }
}

object DbCodeGenerator {

  def run(slickDriver: String, jdbcDriver: String, url: String, outputDir: String, pkg: String, user: Option[String], password: Option[String]): Unit = {
    val driver: JdbcProfile =
      Class.forName(slickDriver + "$").getField("MODULE$").get(null).asInstanceOf[JdbcProfile]
    val dbFactory = driver.api.Database
    val db = dbFactory.forURL(url, driver = jdbcDriver,
      user = user.getOrElse(null), password = password.getOrElse(null), keepAliveConnection = true)
    try {
      val m = Await.result(db.run(driver.createModel(None, false)(ExecutionContext.global).withPinnedSession), Duration.Inf)
      new DbCodeGenerator(m).writeToFile(slickDriver,outputDir,pkg)
    } finally db.close
  }

  def run(uri: URI, outputDir: Option[String]): Unit = {
    val dc = DatabaseConfig.forURI[JdbcProfile](uri)
    val pkg = dc.config.getString("codegen.package")
    val out = outputDir.getOrElse(dc.config.getStringOr("codegen.outputDir", "."))
    val slickDriver = if(dc.driverIsObject) dc.driverName else "new " + dc.driverName
    try {
      val m = Await.result(dc.db.run(dc.driver.createModel(None, false)(ExecutionContext.global).withPinnedSession), Duration.Inf)
      new DbCodeGenerator(m).writeToFile(slickDriver, out, pkg)
    } finally dc.db.close
  }

  def main(args: Array[String]): Unit = {
    args.toList match {
      case uri :: Nil =>
        run(new URI(uri), None)
      case uri :: outputDir :: Nil =>
        run(new URI(uri), Some(outputDir))
      case slickDriver :: jdbcDriver :: url :: outputDir :: pkg :: Nil =>
        run(slickDriver, jdbcDriver, url, outputDir, pkg, None, None)
      case slickDriver :: jdbcDriver :: url :: outputDir :: pkg :: user :: password :: Nil =>
        run(slickDriver, jdbcDriver, url, outputDir, pkg, Some(user), Some(password))
      case _ => {
        println("""
                  |Usage:
                  |  SourceCodeGenerator configURI [outputDir]
                  |  SourceCodeGenerator slickDriver jdbcDriver url outputDir pkg [user password]
                  |
                  |Options:
                  |  configURI: A URL pointing to a standard database config file (a fragment is
                  |    resolved as a path in the config), or just a fragment used as a path in
                  |    application.conf on the class path
                  |  slickDriver: Fully qualified name of Slick driver class, e.g. "slick.driver.H2Driver"
                  |  jdbcDriver: Fully qualified name of jdbc driver class, e.g. "org.h2.Driver"
                  |  url: JDBC URL, e.g. "jdbc:postgresql://localhost/test"
                  |  outputDir: Place where the package folder structure should be put
                  |  pkg: Scala package the generated code should be places in
                  |  user: database connection user name
                  |  password: database connection password
                  |
                  |When using a config file, in addition to the standard config parameters from
                  |slick.backend.DatabaseConfig you can set "codegen.package" and
                  |"codegen.outputDir". The latter can be overridden on the command line.
                """.stripMargin.trim)
        System.exit(1)
      }
    }
  }

}