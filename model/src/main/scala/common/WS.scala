package common

import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.ws.ning.NingWSClient
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object WS {

  lazy val client : NingWSClient = {
    val builder = new AsyncHttpClientConfig.Builder()
    new NingWSClient(builder.build())
  }

  def close(): Unit = {
    client.close()
  }
  
  def batchedRequests[T, U](input: Seq[T], parallelism: Int)(f: T => Future[U]) : Future[Seq[U]] = {
  
    if (input.length > 0) {

      val firstBatch = input.take(parallelism)
      val firstResult = Future.sequence(firstBatch.map { x => f(x) })
      
      for {
        result <- firstResult
        otherResult <- batchedRequests(input.drop(parallelism), parallelism)(f)
      } yield {
        result ++ otherResult
      }
    
    } else {
      Future.successful(Nil)
    }
    
  }

}
