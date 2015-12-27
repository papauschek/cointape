package model

import common.Log
import org.joda.time.DateTime
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object PredictorCache {

  private var cached = Option.empty[Predictor]

  private var lastPredictorUpdate = Option.empty[DateTime]

  def predictor : Option[Predictor] = cached

  def update(): Future[Unit] = {
    val now = DateTime.now()

    // start cache update only every 2 minutes tops
    if (!lastPredictorUpdate.exists(u => now.getMillis - u.getMillis < 120000)) {

      if (lastPredictorUpdate.isEmpty) {
        Log.info("creating predictor")
      } else {
        Log.info("updating predictor")
      }

      lastPredictorUpdate = Some(now)

      Future {
        val cache = BlockCache.update()
        if (cache.preliminaryData) {
          cached = Some(Predictions.getPredictor(cache))
        } else {
          Log.info("not enough blocks for prediction")
        }
      } recover {
        case ex: Exception =>
          Log.error("Could not create predictor.", ex)
      }

    } else {
      Future.successful()
    }
  }

}
