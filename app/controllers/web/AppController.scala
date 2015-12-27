package controllers.web

import common.AccessCache
import model._
import org.joda.time.format.DateTimeFormat
import play.api.cache.Cache
import play.api.libs.json.Json
import play.api.mvc._

object AppController extends Controller {

  def index(page: String) = Action {
    implicit request =>
      Ok(views.html.app.index())
  }

  def notFound(any: String) = Action {
    implicit request =>
      NotFound("Not found")
  }

  def fees = Action {
    implicit request =>

      import play.api.Play.current
      PredictorCache.predictor match {
        case Some(predictor) => Cache.getOrElse("fees", 4)(serveFees(predictor))
        case _ => ServiceUnavailable("Please wait while predictions are being updated.")
      }

  }

  private def serveFees(predictor: Predictor) = {

    val fees = Fees.getFeeStructure()
    val summary = Fees.getFeePredictions(predictor, fees)
    val bestIndex = summary.predictions.indexWhere(_.fee == summary.halfHourFee)
    val secondBestIndex = summary.predictions.indexWhere(_.fee == summary.hourFee)

    Ok(Json.obj(
      "bestIndex" -> bestIndex,
      "maxCount" -> fees.map(_.count).max,
      "maxMemCount" -> fees.map(_.memCount).max,
      "medianTxSize" -> predictor.medianTxSize,
      "fees" -> fees.map {
        fee =>
          val pred = summary.predictions(fee.index)
          val speed = if (fee.index >= bestIndex) 2
          else if (fee.index >= secondBestIndex) 1
          else 0

          Json.obj(
            "minFee" -> fee.minFee,
            "maxFee" -> fee.maxFee,
            "count" -> fee.count,
            "memCount" -> fee.memCount,
            "minDelay" -> pred.minDelay,
            "maxDelay" -> Math.min(10000, pred.maxDelay),
            "minMinutes" -> pred.minMinutes,
            "maxMinutes" -> Math.min(10000, pred.maxMinutes),
            "speed" -> speed)
      })
    )

  }

  def testStatus = Action {
    implicit request =>

      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
      val accesses = AccessCache.accessCounts.toSeq.sortBy(_._1).map {
        case (time, hosts) =>
          formatter.print(time * 3600000) + ": " + hosts.toString
      }.mkString("\r\n")

      Ok(Performance.checkPredictions + "\r\n\r\n" + accesses)
  }

  def testTransaction(hash: String) = Action {
    implicit request =>
      val result = Performance.getTransactionInfo(hash)
      Ok(result)
  }

}
