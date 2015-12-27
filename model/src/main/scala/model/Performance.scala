package model

import db.Tables.{PredictionRow, TxRow}
import db.{DB, Tables}
import org.joda.time.{DateTime}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import DB.Implicits._

object Performance {

  def storePredictions(predictor: Predictor, txs: Seq[TxRow]): Unit = {

    val existingHashes = DB(Tables.Prediction.filter(_.hash.inSet(txs.map(_.hash))).map(_.hash).result).toSet
    val newTxs = txs.filter(tx => !existingHashes(tx.hash))

    if (newTxs.nonEmpty) {

      val cache = BlockCache.blocks.getOrElse(predictor.cache)

      // create new predictions
      val rows = newTxs.map {
        tx =>
          val prediction = Fees.getFeePrediction(predictor, tx.fee)
          val height = cache.getHeightByDate(tx.createDate)
          PredictionRow(tx.hash, height, prediction.minDelay, prediction.maxDelay,
            prediction.minMinutes, prediction.maxMinutes, DB.now)
      }

      DB(Tables.Prediction ++= rows)

      //Log.info("added new predictions: " + rows.length)
    }

  }


  private def predictionRows : Query[(Tables.Prediction, Tables.Tx, Rep[Int]), (Tables.PredictionRow, Tables.TxRow, Int), Seq] = {

    val datePart = SimpleFunction.binary[String,DateTime,Long]("date_part")

    (for {
      prediction <- Tables.Prediction if prediction.createDate > DB.now.minusHours(6)
      tx <- Tables.Tx if tx.hash === prediction.hash && tx.mineDate.isDefined
    } yield {
        // check if prediction successful
        val mineMinutes = datePart("epoch", tx.mineDate.getOrElse(DB.now)) / 60L
        val startMinutes = datePart("epoch", prediction.createDate) / 60L
        val minutes = (mineMinutes - startMinutes).asColumnOf[Int]
        (prediction, tx, minutes)
    })

  }

  private def predictionError(actual: Int, p: PredictionRow): Int = {
    val correctedActual = actual.max(0)
    if (p.minutesMin > correctedActual) correctedActual - p.minutesMin
    else if (p.minutesMax < correctedActual) correctedActual - p.minutesMax
    else 0
  }

  private def predictionSummary() : String = {

    // group success counts by fee
    val predictions = DB((for {
      (prediction, tx, actualMinutes) <- predictionRows
    } yield {
        val normFee = ((tx.fee + 9L) / 10L) * 10L
        (normFee, prediction, actualMinutes)
    }).result)

    // group higher fees into one bucket
    val successByFee = predictions.groupBy(_._1.min(100)).map {
      case (fee, list) =>
        val diffs = list.map { case (_, p, actual) => predictionError(actual, p) }
        val faster = diffs.filter(_ < 0).sortBy(x => x)
        val slower = diffs.filter(_ > 0).sortBy(x => x)
        val fasterMedian = if (faster.isEmpty) 0 else faster(faster.length / 2)
        val slowerMedian = if (slower.isEmpty) 0 else slower(slower.length / 2)
        val (fasterCount, slowerCount, listCount) = (faster.length, slower.length, list.length)
        val successCount = listCount - fasterCount - slowerCount
        (fee, listCount, successCount, fasterCount, slowerCount, fasterMedian, slowerMedian)
    }.toList

    val totalCount = successByFee.map(_._2).sum
    val overallSuccess = (100 * successByFee.map(_._3).sum) / totalCount.max(1)

    val fees = successByFee.sortBy(_._1).map {
      case (fee, listCount, successCount, fasterCount, slowerCount, fasterMedian, slowerMedian) =>
        val percent = (100 * successCount) / listCount
        val fasterPercent = (100 * fasterCount) / listCount
        val slowerPercent = (100 * slowerCount) / listCount
        s"$fee ($listCount, ok:$percent%, faster:$fasterPercent% ($fasterMedian), slower:$slowerPercent% ($slowerMedian))"
    }.mkString("\r\n")

    s"Overall $overallSuccess%\r\n$fees"
  }

  def checkPredictions(): String = {

    // group success counts by fee
    val grouped = DB((for {
      (prediction, tx, actualMinutes) <- predictionRows
    } yield {
      (tx, prediction, actualMinutes)
    }).result)

    val txs = grouped.map {
    case (tx, prediction, actualMinutes) => (tx, prediction, actualMinutes, predictionError(actualMinutes, prediction))
    } filter {
      case (tx, prediction, actualMinutes, error) => error != 0
    } sortBy {
      case (tx, prediction, actualMinutes, error) => -Math.abs(error)
    } take(500) map {
      case (tx, prediction, actualMinutes, error) =>
        //val error = predictionError(actualMinutes, prediction)
        tx.hash + ": " + actualMinutes + " (predicted " + prediction.minutesMin + "-" + prediction.minutesMax + ")"
    }

    val totalPerf = predictionSummary()
    s"$totalPerf\r\n\r\n${txs.mkString("\r\n")}"
  }

  def getTransactionInfo(hash: String) : String = {

    val predictions = DB((for {
      (prediction, tx, actualMinutes) <- predictionRows if tx.hash === hash
      blockTx <- Tables.BlockTx if blockTx.txId === tx.id
      block <- Tables.Block if block.id === blockTx.blockId
    } yield {
        (prediction, tx, block, actualMinutes)
    }).result)


    predictions.toList.toString
  }


}
