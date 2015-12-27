package service

import play.api.libs.json.JsValue
import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/** ugly workaround to prevent bitcoin core from refusing to respond after
  * a couple of parallel requests
  * This makes sure we only send one request at a time.
  */
object CoreApiCalls {

  private val sync = new Object
  private var current = Option.empty[Future[JsValue]]

  def run(future: Unit => Future[JsValue]): Future[JsValue] = {

    sync.synchronized {

      if (current.exists(_.isCompleted)) {
        current = None
      }

      if (current.isEmpty) {
        // run new future and return
        val createdFuture = future()
        current = Some(createdFuture)
        createdFuture
      } else {
        Future.failed(new IllegalStateException("Still waiting for request"))
      }

    }

  }

}
