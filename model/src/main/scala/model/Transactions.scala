package model

import common.{WS, Log}
import db.DB.Implicits._
import db.Tables.TxRow
import db.{DB, Tables}
import org.joda.time.{DateTimeZone, DateTime}
import service.{CoreApi, BlockchainApiTx}
import slick.driver.PostgresDriver.api._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

case class TxItem(createDate: DateTime, fee: Long, size: Int)

object Transactions {

  var lastBlockDownloadDate: DateTime = DateTime.now().minusSeconds(20)

  def updateMempool(): Future[Seq[TxRow]] = {
    CoreApi.unconfirmedTransactions().map {
      txs =>
        //val newTxs = txs.length - countExisting(txs.map(_.hash))
        //Log.info("Storing mempool: " + newTxs)
        updateTxs(txs)
    }
  }

  /** store mempool txs, returning the added rows */
  def updateTxs(txs: Seq[BlockchainApiTx]) : Seq[TxRow] = {

    // get existing txs
    val existing = DB(Tables.Tx.filter(_.hash.inSet(txs.map(_.hash))).result)
    val existingHashes = existing.map(_.hash).toSet
    val newTxs = txs.filter(tx => !existingHashes(tx.hash))

    // insert new transaction
    /* removed par */
    newTxs.map {
      tx => Transactions.storeTx(tx, None)
    }.seq
  }

  /** get or download txs that dont have negative fee */
  def downloadTxs(ids: Seq[String], mineDate: Option[DateTime]): Future[Seq[TxRow]] = {
    lastBlockDownloadDate = DateTime.now
    WS.batchedRequests(Random.shuffle(ids), 1)(hash => downloadTx(hash, mineDate)).map(_.flatten)
  }

  /** get or download a tx */
  def downloadTx(hash: String, mineDate: Option[DateTime]): Future[Option[TxRow]] = {
    DB(Tables.Tx.filter(_.hash === hash).result.headOption) match {
      case Some(dbTx) =>

        val newTx = mineDate match {
          case Some(mineDate) if dbTx.mineDate.isEmpty =>
            // update minedate in db if necessary
            updateMineDate(dbTx.hash, mineDate)
            dbTx.copy(mineDate = Some(mineDate))
          case _ =>
            dbTx
        }

        Future.successful(Some(newTx))

      case _ =>

        // not found in db, need to download and store
        CoreApi.fullTransaction(hash).map {
          fullTx =>

            // mark last download date
            lastBlockDownloadDate = DateTime.now

            // store only if non negative fee
            fullTx.filter(_.absFee >= 0).map {
              tx => storeTx(tx, mineDate)
            }
        }
    }
  }

  def getTx(hash: String) : Option[TxRow] = {
    DB(Tables.Tx.filter(_.hash === hash).result.headOption)
  }

  def updateMineDate(hash: String, mineDate: DateTime) : Boolean = {
    0 < DB(Tables.Tx.filter(t => t.hash === hash && t.mineDate.isEmpty).map(_.mineDate).update(Some(mineDate.withZone(DateTimeZone.UTC))))
  }

  def storeTx(tx: BlockchainApiTx, mineDate: Option[DateTime]) : TxRow = {
    val txRow = TxRow(0, tx.hash, tx.fee, tx.absFee, tx.size, tx.createDate.withZone(DateTimeZone.UTC), mineDate.map(_.withZone(DateTimeZone.UTC)))
    try {
      val id = DB(Tables.Tx returning Tables.Tx.map(_.id) += txRow)
      txRow.copy(id = id)
    } catch {
      case ex: Exception =>
        Log.info("insert race condition, getting existing tx")
        val result = DB(Tables.Tx.filter(_.hash === tx.hash).result.head)
        if (mineDate.isDefined && result.mineDate.isEmpty) updateMineDate(tx.hash, mineDate.get)
        result
    }
  }

  def unconfirmedTransactions() : Seq[TxItem] = {
    DB(Tables.Tx.filter(_.mineDate.isEmpty).
      map(t => (t.createDate, t.fee, t.size)).result).map(TxItem.tupled)
  }

  def unconfirmedTransactionCount() : Int = {
    DB(Tables.Tx.filter(_.mineDate.isEmpty).length.result)
  }

  def recentTransactions(minDate: DateTime) : Seq[TxItem] = {
    DB(Tables.Tx.filter(_.createDate >= minDate).
      map(t => (t.createDate, t.fee, t.size)).result).map(TxItem.tupled)
  }

  def countExisting(txs: Seq[String]) : Int = {
    DB(Tables.Tx.filter(_.hash.inSet(txs)).length.result)
  }

  def transactionsByFeeToday(): Seq[(Long, Int)] = {
    val today = DateTime.now.minusDays(1)
    val fees = DB(Tables.Tx.filter(t => t.mineDate >= today || t.mineDate.isEmpty).
      map(t => ((t.fee + 9L) / 10L) * 10L).
      groupBy(t => t).map { case (fee, rows) => (fee, rows.length) }.result)
    fees
  }

  def transactionsByFeeMempool(): Seq[(Long, Int)] = {
    val fees = DB(Tables.Tx.filter(t => t.mineDate.isEmpty).
      map(t => ((t.fee + 9L) / 10L) * 10L).
      groupBy(t => t).map { case (fee, rows) => (fee, rows.length) }.result)
    fees
  }

}
