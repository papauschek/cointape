package task

import java.util.concurrent.TimeUnit
import common.Log
import db.DB
import model.{Performance, PredictorCache, Blocks, Transactions}
import play.api.Application
import play.api.libs.concurrent.Akka
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try

class Scheduler(app: Application) {

  Akka.system(app).scheduler.schedule(Duration(10, TimeUnit.MINUTES), Duration(10, TimeUnit.MINUTES)) {
    if (!isClosing) {
      every10Minutes()
    } else {
      close()
    }
  }

  Akka.system(app).scheduler.schedule(Duration(60, TimeUnit.SECONDS), Duration(30, TimeUnit.SECONDS)) {
    if (!isClosing) {
      every30Seconds()
    } else {
      close()
    }
  }

  Akka.system(app).scheduler.scheduleOnce(Duration(0, TimeUnit.SECONDS)) {
    afterLaunch()
  }

  private def isClosing = Try(Akka.system(app).isTerminated).getOrElse(true)

  def afterLaunch(): Unit = {
    for {
      _ <- every10Minutes()
      _ <- every30Seconds()
    } yield { }
  }

  /** download new blocks */
  def every30Seconds(): Future[Unit] = {

    Log.info("mempool: " + Transactions.unconfirmedTransactionCount())

    if (Transactions.lastBlockDownloadDate.plusSeconds(20).isBeforeNow)
    {
      Blocks.updateBlocks().map {
        newBlocks =>

          // update predictor for each new block
          if (newBlocks > 0 || PredictorCache.predictor.isEmpty) {
            PredictorCache.update()
          }

          ()
      } recover {
        case ex: Exception => Log.error("Block update error", ex)
      }
    }

    // update mempool and predictions
    for {
      txs <- Transactions.updateMempool()
    } yield {
      // predict transaction delays
      PredictorCache.predictor.foreach(predictor =>
        Performance.storePredictions(predictor, txs))
    }
  }

  /** update predictor */
  private def every10Minutes(): Future[Unit] = {

    Blocks.cleanUp()

    // update predictor if exists
    if (PredictorCache.predictor.isDefined)
      PredictorCache.update()
    else
      Future.successful()
  }

  private def close(): Unit = {
    DB.close()
  }

}
