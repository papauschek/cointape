package model

import common.Log
import db.Tables.{BlockRow, BlockTxRow, TxRow}
import db.{DB, Tables}
import org.joda.time.{DateTimeZone, DateTime}
import service.{CoreApiBlock, CoreApi}
import slick.driver.PostgresDriver.api._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import DB.Implicits._

object Blocks {

  def latestBlock: BlockRow = {
    DB(Tables.Block.sortBy(_.height.desc).take(1).result.head)
  }

  def latestBlocks(count: Int) : IndexedSeq[BlockRow] = {
    val latest = latestBlock
    val minHeight = latest.height - count + 1
    val recent = DB(Tables.Block.filter(_.height >= minHeight).result)
    val blockByHash = recent.map(b => (b.hash, b)).toMap
    var result = ArrayBuffer(latest)
    while (blockByHash.contains(result.last.parentHash)) {
      result += blockByHash(result.last.parentHash)
    }
    result.toArray[BlockRow]
  }

  /** updates blocks, returns number of new blocks downloaded */
  def updateBlocks() : Future[Int] = {
    val maxDepth = 2
    for {
      blockHash <- CoreApi.latestBlock()
      downloadCount <- updateBlocks(blockHash, maxDepth, 0)
      cache = BlockCache.update()
      _ <- if (cache.enoughData) Future.successful(0) else updateBlocks(cache.firstCached.parentHash, 2, 0)
    } yield {

      if (downloadCount > 0) {
        Log.info("Blocks of day: " + BlockCache.update().latestBlocks.length)
      }

      downloadCount
    }
  }

  private def updateBlocks(startHash: String, depth: Int, downloaded: Int) : Future[Int] = {

    val download = getOrDownloadBlock(startHash)
    val downloadCount = if (download.isCompleted) downloaded else downloaded + 1

    for {
      maybeBlock <- download
      finalDepth <- {
        // fetch parent block if necessary
        maybeBlock match {
          case Some(block) if depth > 0 =>
            updateBlocks(block.parentHash, depth - 1, downloadCount)
          case _ =>
            Future.successful(downloadCount)
        }
      }
    } yield {
      finalDepth
    }

  }

  private def getOrDownloadBlock(hash: String) : Future[Option[BlockRow]] = {

    DB(Tables.Block.filter(_.hash === hash).result.headOption) match {
      case Some(dbBlock) =>

        // block already in DB
        Future.successful(Some(dbBlock))

      case _ =>

        // need to download block
        Log.info("Downloading block " + hash)
        for {
          (block, knownTxs) <- verifiedBlock(hash)
          txs <- Transactions.downloadTxs(block.txs, Some(block.createDate))
        } yield {
          val blockRow = storeBlock(block, txs, knownTxs)

          // only return if on main chain
          if (!block.mainChain) Log.info("Block not on main chain: " + hash)
          Some(blockRow).filter(_ => block.mainChain)
        }

    }

  }

  private def verifiedBlock(hash: String): Future[(CoreApiBlock, Int)] = {
    CoreApi.coreBlock(hash).map {
      block =>
        val knownTxs = DB(Tables.Tx.filter(t => t.hash.inSet(block.txs)).length.result)
        (block, knownTxs)
    }
  }

  private def storeBlock(block: CoreApiBlock, txs: Seq[TxRow], knownTxs: Int) : BlockRow = {

    val start = System.currentTimeMillis()
    Log.info("Storing block")

    // store block
    val minFee = if (txs.nonEmpty) txs.map(_.fee).min else Int.MaxValue
    val rawBlockRow = BlockRow(0, block.height, block.hash, block.parentHash, block.createDate.withZone(DateTimeZone.UTC), minFee, txs.length, knownTxs, block.size)

    try {
      val blockId = DB(Tables.Block returning Tables.Block.map(_.id) += rawBlockRow)
      val blockRow = rawBlockRow.copy(id = blockId)

      // connect transactions to block
      DB(Tables.BlockTx ++= txs.map(tx => BlockTxRow(blockId, tx.id)))

      Log.info("Stored block " + block.hash + ", " + (System.currentTimeMillis() - start) + "ms")
      blockRow
    } catch {
      case ex: Exception =>
        Log.error("Race condition while storing block")
        DB(Tables.Block.filter(_.hash === block.hash).result.head)
    }
  }

  def cleanUp(): Unit = {

    val oldestBlock = DateTime.now().minusDays(7)
    val oldestMempool = DateTime.now().minusDays(7)

    val start = System.currentTimeMillis()

    // delete blocks older than 7 days
    val deletedBlocks = Tables.Block.filter(_.createDate < oldestBlock)

    // delete block tx
    val blockTxCount = DB(Tables.BlockTx.filter(_.blockId.in(deletedBlocks.map(_.id))).delete)
    Log.info(s"deleted $blockTxCount transactions (from blocks)")

    // delete tx
    val txCount = DB(Tables.Tx.filter(tx => tx.mineDate.isDefined &&
      !Tables.BlockTx.filter(_.txId === tx.id).exists).delete)
    Log.info(s"deleted $txCount transactions")

    // delete block itself
    val blockCount = DB(deletedBlocks.delete)
    Log.info(s"deleted $blockCount blocks")

    // delete tx mempool
    val memCount = DB(Tables.Tx.filter(tx => tx.mineDate.isEmpty && tx.createDate < oldestMempool).delete)
    Log.info(s"deleted $memCount transactions (mempool)")

    // delete predictions
    val predictionCount = DB(Tables.Prediction.filter(p =>
      !Tables.Tx.filter(_.hash === p.hash).exists).delete)
    Log.info(s"deleted $predictionCount predictions")

    val time = System.currentTimeMillis() - start
    Log.info(s"cleanup completed in $time ms")

  }

}
