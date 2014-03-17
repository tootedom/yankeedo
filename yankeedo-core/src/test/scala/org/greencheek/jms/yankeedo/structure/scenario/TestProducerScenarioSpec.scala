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

import java.util.concurrent._
import akka.actor.{ActorRef, Props, ActorSystem}
import org.greencheek.jms.yankeedo.scenarioexecution.{ScenarioActorSystems, ReturnScenarioActorSystems, StartExecutingScenarios, ScenariosExecutionManager}
import scala.concurrent.duration._
import org.specs2.mutable.Specification
import org.apache.activemq.broker.{TransportConnector, BrokerFactory, BrokerService}
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.greencheek.jms.util.PortUtil
import org.specs2.specification.{Fragments, Step, BeforeAfter}
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.apache.activemq.broker.jmx.ManagementContext
import org.apache.activemq.store.memory.MemoryPersistenceAdapter
import org.greencheek.jms.yankeedo.scenarioexecution.producer.AkkaProducer
import akka.util.Timeout
import scala.concurrent.{ExecutionContext, impl, Await}
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import java.util
import akka.dispatch.Dispatchers
import scala.Some
import akka.pattern.ask
import java.util.concurrent.TimeUnit
import org.apache.activemq.broker.region.{DestinationStatistics, Destination}
import org.apache.activemq.command.ActiveMQDestination
import java.util.{Map => JMap}
import scala.collection.JavaConversions._


/**
 * Created by dominictootell on 09/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class TestProducerScenarioSpec extends BrokerBasedSpec {


  val myContext = WithActorSystem();


  "Producing messages" >> {
    "terminate after 10 messages" in myContext {
      val appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      System.out.println("==========")
      System.out.println("LATCH : " + appLatch)
      System.out.println("==========")
      System.out.flush()

      val scenario = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 10
          produce to queue "queue"
      )

      val scenarioExecutor : ActorRef = actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,ScenarioContainer(scenario))))
      scenarioExecutor ! StartExecutingScenarios

      var ok : Boolean = false
      try {
        ok = appLatch.await(30,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }

      ok should beTrue

      val map = broker.getBroker.getDestinationMap()
      getMessageCountForQueueDestination(map,"queue") should beEqualTo(10)
      getProducerCountForQueueDestination(map,"queue") should beEqualTo(0)



    }

    "terminate after 500 millis" in myContext {

      val appLatch = myContext.latch
      val actorSystem = myContext.actorSystem


      System.out.println("==========")
      System.out.println("LATCH : " + appLatch)
      System.out.println("==========")
      System.out.flush()

      val scenario = createScenario(
        "produce messages for 2 seconds scenario" connect_to "tcp://localhost:" +  port +"?daemon=true&jms.closeTimeout=200"
//          run_for Duration(500,MILLISECONDS)
          until_no_of_messages_sent 100
          produce to queue "my2secondqueue"
      )

      val scenarioExecutor : ActorRef = actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,ScenarioContainer(scenario))))
      scenarioExecutor ! StartExecutingScenarios

      val start : Long = System.currentTimeMillis()
      var ok : Boolean = false
      try {
        ok = appLatch.await(10,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }
      val end : Long = System.currentTimeMillis()-start

      ok should beTrue
      end should be lessThan(5000)

      val map = broker.getBroker.getDestinationMap()
      getMessageCountForQueueDestination(map,"my2secondqueue") should be greaterThan(1)
      getProducerCountForQueueDestination(map,"my2secondqueue") should beEqualTo(0)

    }


    "send messages with a delay " in myContext {

      val appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      val scenario = createScenario(
        "produce messages for 3 seconds scenario, with delay"
        connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          run_for Duration(3,SECONDS)
          until_no_of_messages_sent -1
          produce to queue "delayedqueue"
          with_per_message_delay_of Duration(1,SECONDS)
      )


      val scenarioExecutor : ActorRef =  actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,ScenarioContainer(scenario))))
      scenarioExecutor ! StartExecutingScenarios

      val start : Long = System.currentTimeMillis()
      var ok : Boolean = false
      try {
        ok = appLatch.await(6000,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }
      val end : Long = System.currentTimeMillis()-start

      ok should beTrue
      end should be lessThan(5000)


      val map = broker.getBroker.getDestinationMap()
      getMessageCountForQueueDestination(map,"delayedqueue") should be greaterThan(1)
      getMessageCountForQueueDestination(map,"delayedqueue") should be lessThan(10)
      getProducerCountForQueueDestination(map,"delayedqueue") should beEqualTo(0)

    }

    "send messages over a number of actors" in myContext {

      val appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      val scenario = createScenario(
        "produce messages for across a number of actors" connect_to "tcp://localhost:"+ port +"?daemon=true&jms.closeTimeout=200"
          run_for Duration(4,SECONDS)
          with_no_of_actors 4
          until_no_of_messages_sent -1
          produce to queue "loadbalancedactors"
          with_per_message_delay_of Duration(100,MILLISECONDS)
      )


      val scenarioExecutor : ActorRef =  actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,ScenarioContainer(scenario))),"system")

      implicit val timeout = Timeout(2,SECONDS)
      val future = scenarioExecutor ? ReturnScenarioActorSystems
      val result = Await.result(future, timeout.duration).asInstanceOf[ScenarioActorSystems]

      result should not beNull
      val actorSystemSize = result.actorSystems.size
      actorSystemSize should beEqualTo(1)

      val scenariosActorSystem : ActorSystem =result.actorSystems(0)


      scenarioExecutor ! StartExecutingScenarios

      Thread.sleep(1000)


      val ref1 = Await.result(scenariosActorSystem.actorSelection("akka://scenariosystem-0/user/ProducerExecutor/producermonitor/ProducerActor1").resolveOne()(Timeout.durationToTimeout(Duration(1,SECONDS))).mapTo[ActorRef],Duration.Inf)
      val ref2 = Await.result(scenariosActorSystem.actorSelection("akka://scenariosystem-0/user/ProducerExecutor/producermonitor/ProducerActor2").resolveOne()(Timeout.durationToTimeout(Duration(1,SECONDS))).mapTo[ActorRef],Duration.Inf)
      val ref3 = Await.result(scenariosActorSystem.actorSelection("akka://scenariosystem-0/user/ProducerExecutor/producermonitor/ProducerActor3").resolveOne()(Timeout.durationToTimeout(Duration(1,SECONDS))).mapTo[ActorRef],Duration.Inf)
      val ref4 = Await.result(scenariosActorSystem.actorSelection("akka://scenariosystem-0/user/ProducerExecutor/producermonitor/ProducerActor4").resolveOne()(Timeout.durationToTimeout(Duration(1,SECONDS))).mapTo[ActorRef],Duration.Inf)

      ref1 should not beTheSameAs(ref2)
      ref1 should not beTheSameAs(ref3)
      ref1 should not beTheSameAs(ref4)
      ref2 should not beTheSameAs(ref3)
      ref2 should not beTheSameAs(ref4)
      ref4 should not beTheSameAs(ref3)


      val start : Long = System.currentTimeMillis()
      var ok : Boolean = false
      try {
        ok = appLatch.await(8000,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }
      val end : Long = System.currentTimeMillis()-start

      ok should beTrue
      end should be lessThan(5000)

      val map = broker.getBroker.getDestinationMap()
      getMessageCountForQueueDestination(map,"loadbalancedactors") should be greaterThan(1)
      getProducerCountForQueueDestination(map,"loadbalancedactors") should beEqualTo(0)

    }


    "send messages to a topic" in myContext {

      val appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      val scenario = createScenario(
        "produce messages for 3 seconds scenario, with delay" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          run_for Duration(3,SECONDS)
          until_no_of_messages_sent -1
          produce to topic "delayedtopic"
          with_per_message_delay_of Duration(1,SECONDS)
      )

      val scenarioExecutor : ActorRef = myContext.actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,ScenarioContainer(scenario))))
      scenarioExecutor ! StartExecutingScenarios

      val start : Long = System.currentTimeMillis()
      var ok : Boolean = false
      try {
        ok = appLatch.await(7000,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }
      val end : Long = System.currentTimeMillis()-start

      ok should beTrue
      end should be lessThan(5000)


      val map = broker.getBroker.getDestinationMap()
      getMessageCountForTopicDestination(map,"delayedtopic") should beEqualTo(0)
      getProducerCountForTopicDestination(map,"delayedtopic") should beEqualTo(0)

    }
  }

}

