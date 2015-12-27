package common

import java.io.{PrintWriter, StringWriter}

import org.slf4j.LoggerFactory
import play.api.libs.json.Json

object Log {

  lazy private val logger = LoggerFactory.getLogger("application")

  def info(source: String, msg: String): Unit = {
    logger.info(Json.obj("source" -> source, "message" -> msg).toString)
  }

  def info(msg: String): Unit = {
    logger.info(msg)
  }

  def info(obj: Any): Unit = {
    logger.info(obj.toString)
  }

  def error(msg: String, ex: Throwable): Unit = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    ex.printStackTrace(pw)
    logger.error(s"$msg: $sw")
  }

  def error(msg: String): Unit = {
    logger.error(msg)
  }

}
