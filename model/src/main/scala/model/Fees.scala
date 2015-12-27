package model

case class FeeBucket(index: Int, minFee: Int, maxFee: Int, count: Int, memCount: Int)

case class FeePrediction(fee: Long, minDelay: Int, maxDelay: Int, minMinutes: Int, maxMinutes: Int)

case class FeeSummary(
  predictions: Array[FeePrediction],
  feeByDelay: Map[Int, Long],
  feeByMinutes: Map[Int, Long]) {

  private val delays = feeByDelay.keys.toSeq.sortBy(delay => delay)
  private val minutes = feeByMinutes.keys.toSeq.sortBy(delay => delay)

  val fastestFee: Long = feeByDelay(delays.head).max(feeByMinutes(minutes.head))

  private val halfHourDelay = minutes.filter(_ <= 30).lastOption.getOrElse(minutes.head)
  val halfHourFee: Long = feeByMinutes(halfHourDelay)

  private val hourDelay = minutes.filter(_ <= 60).lastOption.getOrElse(minutes.head)
  val hourFee: Long = feeByMinutes(hourDelay)

}

object Fees {

  def getFeeStructure(): Array[FeeBucket] = {

    val fees = Transactions.transactionsByFeeToday().sortBy(_._1)

    // get bucket size and maximum buckets
    val txCount = fees.map(_._2).sum
    val counts = fees.map(_._2).scan(0)((p, v) => p + v)
    val most = (txCount * 0.95).toInt
    val mostIndex = counts.indexWhere(_ >= most)
    val maxBucket = fees(mostIndex)._1.toInt.max(100) // at least 100 satoshis max

    val bucketSize = (maxBucket / 190 + 1) * 10
    val lastBucket = (maxBucket + bucketSize - 1) / bucketSize
    val maxFee = fees.last._1.toInt

    // group fees into buckets
    val countByBucket = fees.groupBy(f =>
      Math.min((f._1 + bucketSize - 1) / bucketSize, lastBucket)).
      map(f => (f._1, f._2.map(_._2).sum))

    // group mempool fees into buckets
    val memFees = Transactions.transactionsByFeeMempool()
    val memCountByBucket = memFees.groupBy(f =>
      Math.min((f._1 + bucketSize - 1) / bucketSize, lastBucket)).
      map(f => (f._1, f._2.map(_._2).sum))

    // generate bucket metadata
    val buckets = (0 to lastBucket).map {
      index =>
        val bucketCount = countByBucket.getOrElse(index, 0)
        val bucketMemCount = memCountByBucket.getOrElse(index, 0)
        val bucketMax = if (index == lastBucket) maxFee else bucketSize * index
        FeeBucket(index,
          Math.max(0, bucketSize * (index - 1) + 1),
          bucketMax,
          bucketCount,
          bucketMemCount)
    }

    buckets.toArray

  }

  def getFeePredictions(predictor: Predictor, feeBuckets: Array[FeeBucket]) : FeeSummary = {

    val predictions = feeBuckets.map { bucket => getFeePrediction(predictor, bucket.maxFee) }

    /** never predict zero conf transactions */
    val feeByDelay = predictions.groupBy(_.maxDelay).map {
      case (maxDelay, list) => (maxDelay, list.map(_.fee).min.max(10))
    } filter { _._2 < 1000 }

    val feeByMinutes = predictions.groupBy(_.maxMinutes).map {
      case (maxDelay, list) => (maxDelay, list.map(_.fee).min.max(10))
    } filter { _._2 < 1000 }

    FeeSummary(predictions, feeByDelay, feeByMinutes)
  }

  def getFeePrediction(predictor: Predictor, fee: Long) : FeePrediction = {
    val confirmations = predictor.confirmations(fee)
    val delays = confirmations.map(_.delay).sortBy(c => c)
    val minutes = confirmations.map(_.minutes).sortBy(c => c)
    val (minDelay, maxDelay) = getInterval(delays)
    val (minMinutes, maxMinutes) = getInterval(minutes)
    FeePrediction(fee, minDelay, maxDelay, rounded(minMinutes, false), rounded(maxMinutes, true))
  }

  private def rounded(minutes: Int, up: Boolean) : Int = {
    val factor =
      if (minutes <= 60) 5
      else if (minutes <= 120) 10
      else 60

    if (up) ((minutes + factor - 1) / factor) * factor
    else (minutes / factor) * factor
  }

  private def getInterval(confirmations: Array[Int]) : (Int, Int) = {
    val dropCount = confirmations.length / 20
    val min = confirmations(dropCount)
    val max = confirmations(confirmations.length - 1 - dropCount)
    (min, max)
  }

}
