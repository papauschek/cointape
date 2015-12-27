package service

import java.util.concurrent.TimeoutException
import common.{Log, WS}
import org.joda.time.DateTime
import play.api.Play
import play.api.libs.json._
import play.api.libs.ws.WSAuthScheme
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

case class BlockchainApiTx(hash: String, size: Int, absFee: Long, createDate: DateTime) {

  def fee : Long = absFee / size

}

case class CoreApiBlock(hash: String, parentHash: String, height: Int, size: Int,
                              createDate: DateTime, mainChain: Boolean, txs: Seq[String])


case class CoreApiTx(hash: String,  inputs: Seq[(String, Int)], outputs: Array[Long], createDate: DateTime)

/** handle data interface to bitcoin core */
object CoreApi {

  private lazy val coreHost = Play.current.configuration.getString("bitcoin_core.host").getOrElse(???)
  private lazy val coreSecret = Play.current.configuration.getString("bitcoin_core.secret").getOrElse(???)

  def unconfirmedTransactions() : Future[Seq[BlockchainApiTx]] = {
    call("getrawmempool", Json.arr(true)).map {
      response =>
        val obj = response.as[JsObject]
        obj.value.map {
          case (hash, tx) =>
            val size = (tx \ "size").as[Int]
            val absFee = ((tx \ "fee").as[BigDecimal] * 100000000).toLongExact
            val createDate = new DateTime((tx \ "time").as[Long] * 1000)
            BlockchainApiTx(hash, size, absFee, createDate)
        }.toSeq
    }
  }

  /** get hash of latest block */
  def latestBlock() : Future[String] = {
    call("getbestblockhash").map {
      value => value.asOpt[String].getOrElse(throw new IllegalArgumentException(value.toString))
    }
  }

  def fullTransaction(txid: String): Future[Option[BlockchainApiTx]] = {
    //println("downloading " + txid)
    for {
      maybeTx <- transaction(txid)
      prevOut <- maybeTx.map(tx => transactionOutputs(tx).map(l => Some(l))).getOrElse(Future.successful(None))
      size <- transactionSize(txid)
    } yield {
      for {
        tx <- maybeTx
        prevOut <- prevOut
      } yield {
        val absFee = prevOut - tx.outputs.sum
        BlockchainApiTx(tx.hash, size, absFee, tx.createDate)
      }
    }
  }

  def transactionOutputs(tx: CoreApiTx) : Future[Long] = {
    WS.batchedRequests(tx.inputs, 1){
      case (txid, index) =>
        transaction(txid).map(_.map(_.outputs(index)).getOrElse(0L))
    } map { _.sum }
  }

  private def transactionSize(txid: String): Future[Int] = {
    call("getrawtransaction", Json.arr(txid, 0)).map {
      response => response.asOpt[String].getOrElse("").length / 2
    }
  }

  private def transaction(txid: String): Future[Option[CoreApiTx]] = {
    call("getrawtransaction", Json.arr(txid, 1)).map {
      response =>

        if (response == JsNull) {
          None
        } else {
          val hash = (response \ "txid").as[String]

          val inputs = (response \ "vin").as[Seq[JsObject]]
          val outputs = (response \ "vout").as[Seq[JsObject]]

          val txInputs = inputs.flatMap {
            i => (i \ "txid").asOpt[String].map(txid => (txid, (i \ "vout").as[Int]))
          }
          val txOutputs = outputs.map {
            i => ((i \ "value").as[BigDecimal] * 100000000).toLongExact
          }.toArray

          Some(CoreApiTx(hash, txInputs, txOutputs,
            new DateTime((response \ "time").as[Long] * 1000)))
        }
    }
  }

  def coreBlock(hash: String) : Future[CoreApiBlock] = {
    call("getblock", Json.arr(hash)).map {
      response =>
        val hash = (response \ "hash").as[String]
        val parentHash = (response \ "previousblockhash").as[String]
        val createDate = new DateTime((response \ "time").as[Long] * 1000)
        val height = (response \ "height").as[Int]
        val size = (response \ "size").as[Int]
        val confirmations = (response \ "confirmations").as[Int]
        val mainChain = confirmations >= 0
        val txids = (response \ "tx").as[Seq[String]]
        CoreApiBlock(hash, parentHash, height, size, createDate, mainChain, txids)
    }
  }

  /** request data from bitcoin core */
  private def call(method: String, params: JsArray = Json.arr()): Future[JsValue] ={

    CoreApiCalls.run {
      _ =>
        //println(method + params)
        val future = WS.client.url(coreHost).withAuth("", coreSecret, WSAuthScheme.BASIC).
          post(Json.obj("method" -> method, "params" -> params, "id" -> Random.nextInt())).map {
          response =>
            (response.json \ "result").asOpt[JsValue].getOrElse {
              throw new IllegalArgumentException(response.body)
            }
        }

        future.onFailure {
          case ex : TimeoutException =>
            Log.error("Request timed out: " + method + params, ex)
        }

        future
    }

  }
}
