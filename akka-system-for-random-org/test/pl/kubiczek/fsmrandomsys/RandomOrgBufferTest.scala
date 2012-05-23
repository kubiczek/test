package pl.kubiczek.fsmrandomsys

import org.scalatest.matchers.MustMatchers
import org.scalatest.{ BeforeAndAfterEach, WordSpec }
import akka.testkit.AkkaSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.{ BeforeAndAfterEach, WordSpec }
import akka.actor._
import akka.util.duration._
import akka.testkit.TestFSMRef
import pl.kubiczek.randomsystem.RandomRequest
import scala.collection.mutable.Queue

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RandomOrgBufferTest extends AkkaSpec {

  "A RandomOrgBuffer" must {

    "enqueue Random Request to backlog queue when FSM is in Buffering state and buffer is empty" in {
      val fsm = TestFSMRef(new RandomOrgBuffer, "test-fsm-ref-1")
      fsm.setState(Buffering, StateData(Queue(), Queue()))
      fsm.stateName must be(Buffering)
      fsm ! RandomRequest
      fsm.stateName must be(Buffering)
      fsm.stateData match {
        case StateData(_, backlog) => assert(backlog.isEmpty == false, "backlog queue is still empty!")
      }
    }
    
    "response with random number when FSM is in Buffering state and buffer is not empty" in {
      val fsm = TestFSMRef(new RandomOrgBuffer, "test-fsm-ref-1")
      fsm.setState(Buffering, StateData(Queue(52, 32, 12), Queue()))
      fsm.stateName must be(Buffering)
      fsm ! RandomRequest
      fsm.stateName must be(Buffering)
      fsm.stateData match {
        case StateData(buffer, backlog) => 
          assert(buffer.length == 2, "buffer must be dequeue!")
          assert(backlog.isEmpty == true, "backlog queue must be empty!")
      }
    }
  }

  "A TestFSMRef" must {

    "allow access to state data" in {
      val fsm = TestFSMRef(new Actor with FSM[Int, String] {
        startWith(1, "")
        when(1)({
          case Event("go", _) => goto(2) using "go"
          case Event(StateTimeout, _) => goto(2) using "timeout"
        })
        when(2)({
          case Event("back", _) => goto(1) using "back"
        })
      }, "test-fsm-ref-1")
      fsm.stateName must be(1)
      fsm.stateData must be("")
      fsm ! "go"
      fsm.stateName must be(2)
      fsm.stateData must be("go")
      fsm.setState(stateName = 1)
      fsm.stateName must be(1)
      fsm.stateData must be("go")
      fsm.setState(stateData = "buh")
      fsm.stateName must be(1)
      fsm.stateData must be("buh")
      fsm.setState(timeout = 100 millis)
      within(80 millis, 500 millis) {
        (
          awaitCond(fsm.stateName == 2 && fsm.stateData == "timeout"))
      }
    }
  }

}