import _root_.db.DB
import common.WS
import org.slf4j.LoggerFactory
import play.api._
import play.api.mvc._
import task.Scheduler
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Global extends WithFilters(LoggingFilter) {

  override def onStart(app: Application)
  {
    super.onStart(app)
    new Scheduler(app)
  }

  override def onStop(app: Application): Unit =  {
    super.onStop(app)
    DB.close()
    WS.close()
  }

}

object LoggingFilter extends Filter {

  lazy private val logger = LoggerFactory.getLogger("access")

  def apply(nextFilter: (RequestHeader) => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    if (requestHeader.uri.startsWith("/assets") ||
      requestHeader.uri.startsWith("/versioned") ||
      requestHeader.uri.startsWith("/content")) {
      nextFilter(requestHeader)
    } else {
      val startTime = System.currentTimeMillis
      nextFilter(requestHeader).map { result =>
        val endTime = System.currentTimeMillis
        val requestTime = endTime - startTime
        logger.info(s"${requestHeader.method} ${requestHeader.uri} " +
          s"- ${requestTime}ms - status ${result.header.status}")
        result
      }
    }

  }
}
