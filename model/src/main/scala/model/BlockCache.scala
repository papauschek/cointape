package model

import db.Tables.BlockRow
import org.joda.time.{DateTime, Minutes}

/** cache of blocks of last 24 hours */
class BlockCache(
  val latestBlocks: IndexedSeq[BlockRow]) {

  val latest : BlockRow = latestBlocks.head
  val firstCached : BlockRow = latestBlocks.last

  def currentData: Boolean = latest.createDate.isAfter(DateTime.now().minusHours(12))
  def enoughData: Boolean = currentData && firstCached.createDate.isBefore(DateTime.now().minusHours(12))
  def preliminaryData: Boolean = currentData && firstCached.createDate.isBefore(DateTime.now().minusHours(1))

  /** probability of confirming zero fee transactions */
  lazy val zeroFeeTxMineProb : Double = {
    latestBlocks.count(_.minFee == 0) / latestBlocks.length.toDouble
  }

  /** probability of mining empty block (no transactions) */
  lazy val emptyBlockProb : Double = {
    latestBlocks.count(_.txCount == 0) / latestBlocks.length.toDouble
  }

  /** avg tx size in bytes */
  lazy val averageTransactionSize: Int = {
    val txBlocks = latestBlocks.filter(_.txCount > 0)
    val txCount = txBlocks.map(_.txCount).sum
    val txSize = txBlocks.map(_.size).sum
    txSize / txCount.max(1)
  }

  /** estimate transaction height by create date */
  def getHeightByDate(createDate: DateTime) : Int = {
    latestBlocks.find(b => b.createDate.isBefore(createDate)) match {
      case Some(beforeBlock) =>
        beforeBlock.height + 1 // return exact height
      case _ =>
        // estimated height
        val minutesDiff = Minutes.minutesBetween(createDate, firstCached.createDate).getMinutes
        val heightDiff = minutesDiff / 10
        firstCached.height - heightDiff
    }
  }

}

object BlockCache {

  private var blockCache = Option.empty[BlockCache]

  def blocks : Option[BlockCache] = blockCache

  def update(): BlockCache = {
    val result = new BlockCache(Blocks.latestBlocks(6 * 24))
    blockCache = Some(result)
    result
  }

}
