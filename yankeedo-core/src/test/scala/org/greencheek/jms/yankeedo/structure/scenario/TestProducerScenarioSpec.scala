/**
 * Copyright 2012-2013 greencheek.org (www.greencheek.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greencheek.jms.yankeedo.structure.scenario

import java.util.concurrent.{TimeUnit, CountDownLatch}
import akka.actor.{ActorRef, Props, ActorSystem}
import org.greencheek.jms.yankeedo.scenarioexecution.{StartExecutingScenarios, ScenariosExecutionManager}
import scala.concurrent.duration._
import org.specs2.mutable.Specification
import org.apache.activemq.broker.BrokerService
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.greencheek.jms.util.PortUtil
import org.specs2.specification.{Step, BeforeAfter}
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.apache.activemq.broker.jmx.ManagementContext
import org.apache.activemq.store.memory.MemoryPersistenceAdapter
import org.greencheek.jms.yankeedo.scenarioexecution.producer.AkkaProducer
import akka.util.Timeout
import scala.concurrent.Await


/**
 * Created by dominictootell on 09/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class TestProducerScenarioSpec extends Specification {
  sequential

  case class withBroker() extends BeforeAfter {
    var broker : BrokerService = null
    val port : Int = PortUtil.findFreePort
    val jmxport : Int = PortUtil.findFreePort
    var appLatch : CountDownLatch = null
    var actorSystem : ActorSystem = null
    var scenarioExecutor : ActorRef = null

    def before = {
      broker = new BrokerService()
      broker.setPersistent(false)
      broker.setUseJmx(true)

      val mctx = new ManagementContext(java.lang.management.ManagementFactory.getPlatformMBeanServer)
      mctx.setCreateMBeanServer(false)
      mctx.setRmiServerPort(jmxport)
      mctx.setConnectorPort(jmxport)
      mctx.setCreateConnector(false)
      mctx.setUseMBeanServer(false)
      mctx.setFindTigerMbeanServer(false)
      mctx.start()
      broker.setManagementContext(mctx)
      broker.setPersistenceAdapter(new MemoryPersistenceAdapter())
      broker.addConnector("tcp://localhost:" + port);
      broker.setTmpDataDirectory(null)
      broker.setStartAsync(false)

      broker.start()
      broker.waitUntilStarted();

      appLatch = new CountDownLatch(1)
      actorSystem = ActorSystem()
    }
    def after  = {
      broker.stop()

      actorSystem.shutdown()
    }
  }

  val myContext = withBroker();

  "Producing messages" should {
    sequential

    "terminate after 10 messages" in myContext {
      val scenario = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" +  myContext.port
          until_no_of_messages_sent 10
          produce to queue "queue"
      )

      myContext.scenarioExecutor = myContext.actorSystem.actorOf(Props(new ScenariosExecutionManager(myContext.appLatch,ScenarioContainer(scenario))))
      myContext.scenarioExecutor ! StartExecutingScenarios

      var ok : Boolean = false
      try {
        ok = myContext.appLatch.await(10,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }

      ok should beTrue

      myContext.broker.getAdminView.getTotalMessageCount should beEqualTo(10)
      myContext.broker.getAdminView.getTotalProducerCount should beEqualTo(0)



    }

    "terminate after 10 seconds" in myContext {
      val scenario = createScenario(
        "produce messages for 2 seconds scenario" connect_to "tcp://localhost:" +  myContext.port
          run_for Duration(2,SECONDS)
          until_no_of_messages_sent -1
          produce to queue "my2secondqueue"
      )

      myContext.scenarioExecutor = myContext.actorSystem.actorOf(Props(new ScenariosExecutionManager(myContext.appLatch,ScenarioContainer(scenario))))
      myContext.scenarioExecutor ! StartExecutingScenarios

      val start : Long = System.currentTimeMillis()
      var ok : Boolean = false
      try {
        ok = myContext.appLatch.await(10,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }
      val end : Long = System.currentTimeMillis()-start

      ok should beTrue
      end should be lessThan(5000)

      myContext.broker.getAdminView.getTotalMessageCount should be greaterThan(1)
      System.out.println("count" + myContext.broker.getAdminView.getTotalMessageCount )
      myContext.broker.getAdminView.getQueues.filter(name => name.toString.contains("my2secondqueue")).size should be greaterThan(0)
      myContext.broker.getAdminView.getTotalProducerCount should beEqualTo(0)



    }

    "send messages with a delay " in myContext {
      val scenario = createScenario(
        "produce messages for 3 seconds scenario, with delay" connect_to "tcp://localhost:" +  myContext.port
          run_for Duration(3,SECONDS)
          until_no_of_messages_sent -1
          produce to queue "delayedqueue"
          with_per_message_delay_of Duration(1,SECONDS)
      )

      myContext.scenarioExecutor = myContext.actorSystem.actorOf(Props(new ScenariosExecutionManager(myContext.appLatch,ScenarioContainer(scenario))))
      myContext.scenarioExecutor ! StartExecutingScenarios

      val start : Long = System.currentTimeMillis()
      var ok : Boolean = false
      try {
        ok = myContext.appLatch.await(10,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }
      val end : Long = System.currentTimeMillis()-start

      ok should beTrue
      end should be lessThan(5000)

      myContext.broker.getAdminView.getTotalMessageCount should be greaterThan(1)
      myContext.broker.getAdminView.getTotalMessageCount should be lessThan(5)
      myContext.broker.getAdminView.getQueues.filter(name => name.toString.contains("delayedqueue")).size should be greaterThan(0)
      myContext.broker.getAdminView.getTotalProducerCount should beEqualTo(0)



    }

    "send messages over a number of actors" in myContext {
      val scenario = createScenario(
        "produce messages for across a number of actors" connect_to "tcp://localhost:"+myContext.port
          run_for Duration(4,SECONDS)
          with_no_of_actors 4
          until_no_of_messages_sent -1
          produce to queue "loadbalancedactors"
      )

      myContext.scenarioExecutor = myContext.actorSystem.actorOf(Props(new ScenariosExecutionManager(myContext.appLatch,ScenarioContainer(scenario))),"system")
      myContext.scenarioExecutor ! StartExecutingScenarios

      Thread.sleep(3000)

//      myContext.broker.getAdminView.getTotalProducerCount should beEqualTo(1)

      val ref1 = Await.result(myContext.actorSystem.actorSelection("akka://default/user/system/ProducerExecutor/producermonitor/ProducerActor1").resolveOne()(Timeout.durationToTimeout(Duration(1,SECONDS))).mapTo[ActorRef],Duration.Inf)
      val ref2 = Await.result(myContext.actorSystem.actorSelection("akka://default/user/system/ProducerExecutor/producermonitor/ProducerActor2").resolveOne()(Timeout.durationToTimeout(Duration(1,SECONDS))).mapTo[ActorRef],Duration.Inf)
      val ref3 = Await.result(myContext.actorSystem.actorSelection("akka://default/user/system/ProducerExecutor/producermonitor/ProducerActor3").resolveOne()(Timeout.durationToTimeout(Duration(1,SECONDS))).mapTo[ActorRef],Duration.Inf)
      val ref4 = Await.result(myContext.actorSystem.actorSelection("akka://default/user/system/ProducerExecutor/producermonitor/ProducerActor4").resolveOne()(Timeout.durationToTimeout(Duration(1,SECONDS))).mapTo[ActorRef],Duration.Inf)



      ref1 should not beTheSameAs(ref2)
      ref1 should not beTheSameAs(ref3)
      ref1 should not beTheSameAs(ref4)
      ref2 should not beTheSameAs(ref3)
      ref2 should not beTheSameAs(ref4)
      ref4 should not beTheSameAs(ref3)


      val start : Long = System.currentTimeMillis()
      var ok : Boolean = false
      try {
        ok = myContext.appLatch.await(10,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }
      val end : Long = System.currentTimeMillis()-start

      ok should beTrue
      end should be lessThan(5000)

      myContext.broker.getAdminView.getTotalMessageCount should be greaterThan(1)
      myContext.broker.getAdminView.getQueues.filter(name => name.toString.contains("loadbalancedactors")).size should be greaterThan(0)
      myContext.broker.getAdminView.getTotalProducerCount should beEqualTo(0)



    }


  }


}
