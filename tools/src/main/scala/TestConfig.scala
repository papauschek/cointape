import _root_.db.DB
import play.api._
import play.api.mvc.Handler
import java.io.File
import com.typesafe.config.ConfigFactory
import slick.driver.PostgresDriver.api._

object Application {

  def getDev : FakeApplication = {
    getApp(new File("conf/application.conf"))
  }

  /*
  def getProd : FakeApplication = {
    getApp(new File("conf/application.prod.conf"))
  }

  def getTest : FakeApplication = {
    getApp(new File("conf/application.test.conf"))
  }
  */

  private def getApp(configFile: File) : FakeApplication = {
    if (!configFile.exists())
      throw new Exception("Config file does not exist: " + configFile.getAbsolutePath)

    val conf = Configuration(ConfigFactory.parseFile(configFile))
    FakeApplication(configuration = conf)
  }



}

case class FakeApplication(
                            override val path: java.io.File = new java.io.File("."),
                            override val classloader: ClassLoader = classOf[FakeApplication].getClassLoader,
                            val additionalPlugins: Seq[String] = Nil,
                            val withoutPlugins: Seq[String] = Nil,
                            override val configuration: Configuration,
                            val withGlobal: Option[play.api.GlobalSettings] = None,
                            val withRoutes: PartialFunction[(String, String), Handler] = PartialFunction.empty) extends {
  override val sources = None
  override val mode = play.api.Mode.Dev
} with Application with WithDefaultConfiguration with WithDefaultGlobal with WithDefaultPlugins {

  lazy val database : Database = {
    val dbConfig = configuration.getConfig("db.default").getOrElse(???)
    Database.forURL(
      dbConfig.getString("jdbcUrl").getOrElse(???),
      dbConfig.getString("username").getOrElse(???),
      dbConfig.getString("password").getOrElse(???),
      driver = dbConfig.getString("driverClassName").getOrElse(???)
    )
  }

  def connectDatabase(): Unit = {
    DB.dataSource = Some(database)
  }

}
