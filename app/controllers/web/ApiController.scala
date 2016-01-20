package controllers.web

import common.AccessCache
import controllers.web.AppController._
import model.{Fees, Predictor, PredictorCache}
import play.api.cache.Cache
import play.api.libs.json.Json
import play.api.{Mode, Play}
import play.api.mvc.{Result, Request, Action, Controller}

object ApiController extends Controller {

  /** rate limiting and access check for all api requests */
  def handleApiRequest(key: String)(implicit request: Request[_]): Option[Result] = {

    val addresses = request.headers.get("X-Forwarded-For").getOrElse("127.0.0.1")
    val ipAddress = addresses.split(", ").head

    if (//Play.current.mode == Mode.Prod &&
      !request.host.startsWith("api") && // api.cointape.com (deprecated API endpoint)
      !request.uri.startsWith("/api")) { // new api endpoint (/api/...)
      Some(NotFound("Not found"))
    } else if (!AccessCache.canAccess(key, ipAddress)) {
      Some(TooManyRequest("Reached limit of 5000 requests per hour."))
    } else {
      None
    }
  }

  def recommendedFee = Action {
    implicit request =>
      import play.api.Play.current
      handleApiRequest("/fees/recommended").getOrElse {
        PredictorCache.predictor match {
          case Some(predictor) => Cache.getOrElse("/fees/recommended", 4)(serveFees(predictor))
          case _ => ServiceUnavailable("Please wait while predictions are being updated.")
        }
      }
  }

  private def serveFees(predictor: Predictor): Result = {
    val fees = Fees.getFeeStructure()
    val summary = Fees.getFeePredictions(predictor, fees)

    Ok(Json.obj(
      "fastestFee" -> summary.fastestFee,
      "halfHourFee" -> summary.halfHourFee,
      "hourFee" -> summary.hourFee
    ))
  }

  def listFees = Action {
    implicit request =>
      handleApiRequest("/fees/list").getOrElse {
        import play.api.Play.current
        PredictorCache.predictor match {
          case Some(predictor) => Cache.getOrElse("/fees/list", 4)(serveFeeSummary(predictor))
          case _ => ServiceUnavailable("Please wait while predictions are being updated.")
        }
      }
  }

  private def serveFeeSummary(predictor: Predictor) : Result = {

    val fees = Fees.getFeeStructure()
    val summary = Fees.getFeePredictions(predictor, fees)

    Ok(Json.obj(
      "fees" -> fees.map {
        fee =>
          val pred = summary.predictions(fee.index)
          Json.obj(
            "minFee" -> fee.minFee,
            "maxFee" -> fee.maxFee,
            "dayCount" -> fee.count,
            "memCount" -> fee.memCount,
            "minDelay" -> pred.minDelay,
            "maxDelay" -> Math.min(10000, pred.maxDelay),
            "minMinutes" -> pred.minMinutes,
            "maxMinutes" -> Math.min(10000, pred.maxMinutes))
      })
    )

  }


}
