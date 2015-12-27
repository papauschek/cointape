package db

import java.sql.{BatchUpdateException, SQLException}
import _root_.common.Log
import com.github.tototoshi.slick.GenericJodaSupport
import org.joda.time.{DateTime, DateTimeZone}
import slick.dbio.{DBIOAction, NoStream}
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object DB {

  var dataSource : Option[Database] = None

  private lazy val database : Database = dataSource.getOrElse(Database.forDataSource(play.api.db.DB.getDataSource("default")(play.api.Play.current)))

  def apply[R](a: DBIOAction[R, NoStream, Nothing]): R = {
    try {
      Await.result(database.run(a), Duration.Inf)
    } catch {
      case ex : Exception => throw new SQLException("Query Error", logException(ex))
    }

  }

  def future[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = {
    database.run(a) recover {
      case ex : Exception => throw new SQLException("Query Error", logException(ex))
    }
  }

  private def logException(ex: Exception): Exception = {
    ex match {
      case updateEx: BatchUpdateException =>
        val actualEx = Option(updateEx.getNextException).getOrElse(ex)
        Log.error("Query error", actualEx)
        actualEx
      case _ =>
        Log.error("Query error", ex)
        ex
    }
  }

  def now: DateTime = DateTime.now().withZone(DateTimeZone.UTC)

  def close(): Unit = {
    database.close()
  }

  object Implicits extends GenericJodaSupport(PostgresDriver) {


  }

}
