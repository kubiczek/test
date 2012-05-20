package pl.kubiczek.randomsystem

import akka.actor.Actor
import scala.collection.immutable.Stack
import akka.event.LoggingReceive
import java.net.URL
import scala.io.Source

case class FetchFromRandomOrg(batchSize: Int)
case class RandomOrgServerResponse(randomNumbers: List[Int])

class RandomOrgClient extends Actor {

  protected def receive = LoggingReceive {
    case FetchFromRandomOrg(batchSize) =>
      val url = new URL("https://www.random.org/integers/?num=" + batchSize + "&min=0&max=65535&col=1&base=10&format=plain&rnd=new")
      val connection = url.openConnection()
      val stream = Source.fromInputStream(connection.getInputStream)
      sender ! RandomOrgServerResponse(stream.getLines().map(_.toInt).toList)
      stream.close()
  }

}