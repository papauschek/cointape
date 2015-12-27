package model

import java.util.concurrent.TimeUnit
import common.Log
import org.joda.time.{Minutes, DateTime}
import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Random

case class SimBlock(minFee: Long, minProb: Double, size: Int, minutes: Int)

case class Confirmation(delay: Int, minutes: Int)

case class Simulation(blocks: Array[SimBlock]) {

  def confirmation(fee: Long, random: Random): Confirmation = {
    var index = 0
    var confirmed = false
    var minutes = 0
    while(index < blocks.length && !confirmed) {
      val block = blocks(index)
      minutes += block.minutes
      if (fee > block.minFee ||
        (fee == block.minFee && random.nextDouble() < block.minProb)) {
        confirmed = true
      } else {
        index += 1
      }
    }

    if (confirmed) Confirmation(index, minutes) else Confirmation(10000, 10000)
  }

}

case class Predictor(cache: BlockCache, simulations: Array[Simulation], medianTxSize: Int) {

  def confirmations(fee: Long) : Array[Confirmation] = {
    val random = new Random(simulations.head.blocks.head.size)
    simulations.map(s => s.confirmation(fee, random))
  }

}

object Predictions {

  private val longTerm = FiniteDuration(24, TimeUnit.HOURS)
  private val shortTerm = FiniteDuration(3, TimeUnit.HOURS)

  def getPredictor(cache: BlockCache): Predictor = {

    val start = System.currentTimeMillis()

    val minDateLong = DateTime.now().minusHours(longTerm.toHours.toInt)
    val txsLongTerm = Transactions.recentTransactions(minDateLong).toArray

    val minDateShort = DateTime.now().minus(shortTerm.toMillis)
    val txsShortTerm = txsLongTerm.filter(_.createDate.isAfter(minDateShort))

    // simulate
    val unconfirmed = Transactions.unconfirmedTransactions().toArray
    //val unconfirmed = txsShortTerm.take(0)

    Log.info("long: " + txsLongTerm.length)
    Log.info("short: " + txsShortTerm.length)

    val deepSims = (0 until 1000).par.map(_ =>
        simulate(cache, shortTerm, longTerm, txsShortTerm, txsLongTerm, unconfirmed, 6 * 3))
    val shallowSims = (0 until 100).par.map(_ =>
      simulate(cache, shortTerm, longTerm, txsShortTerm, txsLongTerm, unconfirmed, 6 * 24))

    // extend deep (short term) sims with shallow (long term) sim data
    val sims = deepSims.zipWithIndex.map {
      case (sim, index) =>
        Simulation(sim.blocks ++ shallowSims(index % shallowSims.length).blocks.drop(sim.blocks.length))
    }

    //println("simulator: " + (System.currentTimeMillis() - start))

    val sorted = txsLongTerm.sortBy(_.size)
    val medianTxSize = sorted(sorted.length / 2)
    val result = Predictor(cache, sims.toArray, medianTxSize.size)
    //println("noConf: " + cache.emptyBlockProb)
    //println("conf: " + result.confirmations(200).count(_ > 0) / result.simulations.length.toDouble)
    result
  }

  private def simulate(cache: BlockCache,
               shortTerm: Duration, longTerm: Duration,
               txsShortTerm: Array[TxItem], txsLongTerm: Array[TxItem],
               initialMemPool: Array[TxItem], simBlockCount: Int): Simulation = {

    val blocks = mutable.ArrayBuffer.empty[SimBlock]
    var unconfirmed = initialMemPool

    // simulate first 3 hours in 10 minute blocks
    val firstBlocks = shortTerm.toMinutes.toInt / 10
    unconfirmed = simulate(cache, blocks, firstBlocks, txsShortTerm, shortTerm, unconfirmed)

    // simulate next day in 10 minute blocks
    unconfirmed = simulate(cache, blocks, simBlockCount - firstBlocks, txsLongTerm, longTerm, unconfirmed)

    Simulation(blocks.toArray)
  }

  /** simulate a number of blocks with given parameters */
  private def simulate(cache: BlockCache, blocks: mutable.ArrayBuffer[SimBlock], blockCount: Int,
                       base: Array[TxItem], baseDuration: Duration,
                       initialMemPool: Array[TxItem]): Array[TxItem] = {
    var unconfirmed = initialMemPool
    for (_ <- 0 until blockCount) {

      val (unconfPool, fullBlock, miningMinutes) = simulatePool(cache, base, baseDuration)

      val (block, unconf) =
        if (fullBlock) simulateBlock(cache, unconfirmed ++ unconfPool, miningMinutes)
        else (SimBlock(Int.MaxValue, 0, 1000, miningMinutes), unconfirmed)

      unconfirmed = unconf
      blocks += block
    }
    unconfirmed
  }

  private def simulatePool(cache: BlockCache, base: Array[TxItem], baseDuration: Duration): (Array[TxItem], Boolean, Int) = {

    // simulate mining block time
    val blockIndex = Random.nextInt(cache.latestBlocks.length - 1)
    val block = cache.latestBlocks(blockIndex)
    val prevBlock = cache.latestBlocks(blockIndex + 1)
    val miningMinutesRaw = Minutes.minutesBetween(prevBlock.createDate, block.createDate).getMinutes
    val miningMinutes = Math.max(0, miningMinutesRaw)

    // fill memory pool while mining
    val transactionsPerMinute = Math.max(1, base.length / baseDuration.toMinutes.toInt)
    val simCount = transactionsPerMinute * Math.max(1, miningMinutes)
    val generated = Array.fill(simCount)(base(Random.nextInt(base.length)))

    val fullBlock = block.txCount > 0
    (generated, fullBlock, miningMinutes)
  }

  private def simulateBlock(cache: BlockCache, unconfirmed: Array[TxItem], miningMinutes: Int): (SimBlock, Array[TxItem]) = {

    val sorted = unconfirmed.sortBy(- _.fee)
    val newMemPool = mutable.ArrayBuffer.empty[TxItem]
    var blockSize = 0
    var index = 0
    var minFee = Long.MaxValue
    var minConfirms = 0
    var minCount = 1
    val maxBlockSize = 900000
    val zeroConfirm = Random.nextDouble() < cache.zeroFeeTxMineProb

    while (blockSize < maxBlockSize && index < sorted.length) {

      val tx = sorted(index)

      val canConfirm = tx.fee > 0 || zeroConfirm

      if (canConfirm && blockSize + tx.size <= maxBlockSize) {
        // take transaction
        blockSize += tx.size

        if (minFee == tx.fee) {
          minConfirms += 1
          minCount += 1
        } else if (newMemPool.length == 0) {
          minFee = tx.fee
          minCount = 1
          minConfirms = 1
        }

      } else {
        // transaction not mined
        newMemPool += tx
        if (minFee == tx.fee) {
          minCount += 1
        }
      }
      index += 1
    }

    (SimBlock(minFee, minConfirms / minCount.toDouble, blockSize, miningMinutes), newMemPool.toArray)

  }

}
