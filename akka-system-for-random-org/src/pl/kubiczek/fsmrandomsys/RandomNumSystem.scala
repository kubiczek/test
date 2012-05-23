package pl.kubiczek.fsmrandomsys
import scala.collection.mutable.Queue
import java.net.URL
import scala.io.Source
import akka.actor.ActorSystem
import akka.actor.Props
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.ask
import akka.dispatch.Await
import pl.kubiczek.randomsystem.RandomRequest
import com.typesafe.config.ConfigFactory

object RandomNumSystem {

  def main(args: Array[String]): Unit = {
    val customConf = ConfigFactory.parseString("""
      akka { 
        loglevel = DEBUG
        # Log the complete configuration at INFO level when the actor system is started.
    	# This is useful when you are uncertain of what configuration is used.
        #log-config-on-start = on
        actor {
          debug {
            # enable function of LoggingReceive, which is to log any received message at
            # DEBUG level
            receive = on
            # enable DEBUG logging of all LoggingFSMs for events, transitions and timers
        	fsm = on
            #  enable DEBUG logging of actor lifecycle changes
            lifecycle = on
          }
        }
      }
      """)
    val system = ActorSystem("Akka", ConfigFactory.load(customConf))
    val randomOrgBuffer = system.actorOf(Props[RandomOrgBuffer], "randomOrg")

    for (i <- 1 to 102) {
      // asynchronous communication, tell mode
      //randomOrgBuffer ! RandomRequest

      // synchronous communication, ask mode      
      implicit val timeout = Timeout(5 seconds)
      val future = randomOrgBuffer ? RandomRequest
      val veryRandom: Int = Await.result(future.mapTo[Int], 1 minute)
      //println(veryRandom)

      // go sleep for a while before next iteration of for-loop
      Thread.sleep(5)
    }

    system.shutdown()
  }

}