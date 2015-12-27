package common

import play.api.Play.current
import akka.actor.{Props, ActorRef, Actor}
import play.api.libs.concurrent.Akka


/** log API access by IP address and prevent flooding */
object AccessCache {

  private val cache = new AccessCache

  lazy private val instance : ActorRef = Akka.system.actorOf(Props(
    classOf[AccessCacheActor], cache), name = "access_cache")

  def canAccess(key: String, host: String) : Boolean = {
    if (cache.canAccess(key, host)) {
      instance ! (key, host)
      true
    } else {
      false
    }
  }

  def accessCounts : Map[Long, Map[String, Map[String, Int]]] = cache.dates

}

class AccessCache {

  // dates, hosts, keys
  private var dates = Map.empty[Long, Map[String, Map[String, Int]]]

  def currentSlot : Long = System.currentTimeMillis() / 3600000

  def canAccess(key: String, host: String) : Boolean = {
    val date = dates.getOrElse(currentSlot, Map.empty)
    val hostMap = date.getOrElse(host, Map.empty)
    hostMap.getOrElse(key, 0) < 5000
  }

  // update cache from actor thread
  def add(key: String, host: String): Unit = {

    // get current count
    val slot = currentSlot
    val date = dates.getOrElse(slot, Map.empty)
    val hostMap = date.getOrElse(host, Map.empty)

    // update access count
    val count = hostMap.getOrElse(key, 0) + 1
    val newHost = hostMap.updated(key, count)
    val newDate = date.updated(host, newHost)
    dates = dates.updated(slot, newDate)

    // cleanup old dates
    if (dates.size > 168) {
      val minSlot = slot - 168
      dates = dates.filter(_._1 > minSlot)
    }
  }

}

class AccessCacheActor(cache: AccessCache) extends Actor {

  def receive = {
    case (key: String, host: String) => cache.add(key, host)
  }

}