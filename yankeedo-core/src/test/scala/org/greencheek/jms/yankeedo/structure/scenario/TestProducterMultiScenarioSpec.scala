/**
 * Copyright 2012-2014 greencheek.org (www.greencheek.org)
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

import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import akka.actor.{Props, ActorRef}
import org.greencheek.jms.yankeedo.scenarioexecution.{ReturnScenarioActorSystems, ScenarioActorSystems, StartExecutingScenarios, ScenariosExecutionManager}
import java.util.concurrent.TimeUnit
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import org.greencheek.jms.yankeedo.scenarioexecution.producer.message.CamelMessageSource
import akka.camel.CamelMessage

/**
 * Created by dominictootell on 14/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class TestProducterMultiScenarioSpec extends BrokerBasedSpec {

  val myContext = WithActorSystem();


  "Producing messages" >> {
    "terminate after 25 messages" in myContext {
      val appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      System.out.println("==========")
      System.out.println("LATCH : " + appLatch)
      System.out.println("==========")
      System.out.flush()



      val producerScenario1 = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 10
          produce to queue "queue"
      )

      val producerScenario2 = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 15
          produce to queue "queue2"
      )

      val scenarioExecutor : ActorRef = actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,ScenarioContainer(producerScenario1,producerScenario2))))
      scenarioExecutor ! StartExecutingScenarios

      implicit val timeout = Timeout(2,SECONDS)
      val future = scenarioExecutor ? ReturnScenarioActorSystems
      val result = Await.result(future, timeout.duration).asInstanceOf[ScenarioActorSystems]

      result should not beNull
      val actorSystemSize = result.actorSystems.size
      actorSystemSize should beEqualTo(2)

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
      getMessageCountForQueueDestination(map,"queue2") should beEqualTo(15)
      getProducerCountForQueueDestination(map,"queue") should beEqualTo(0)
      getProducerCountForQueueDestination(map,"queue2") should beEqualTo(0)

    }


    "consumes 5 messages" in myContext {
      val appLatch = myContext.latch
      val actorSystem = myContext.actorSystem


      val producerScenario1 = createScenario(
        "consumer 10 messages" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_consumed 5
          consume from queue "consumerqueue"
          prefetch 1
      )

      val producerScenario2 = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 10
          produce to queue "consumerqueue"
          with_message_source HelloStringWithTime()
      )


      val scenarioExecutor : ActorRef = actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,ScenarioContainer(producerScenario1,producerScenario2))))
      scenarioExecutor ! StartExecutingScenarios

      implicit val timeout = Timeout(2,SECONDS)
      val future = scenarioExecutor ? ReturnScenarioActorSystems
      val result = Await.result(future, timeout.duration).asInstanceOf[ScenarioActorSystems]

      result should not beNull
      val actorSystemSize = result.actorSystems.size
      actorSystemSize should beEqualTo(2)

      var ok : Boolean = false
      try {
        ok = appLatch.await(30,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }

      ok should beTrue

      val map = broker.getBroker.getDestinationMap()
      getMessageCountForQueueDestination(map,"consumerqueue") should beEqualTo(5)
      getProducerCountForQueueDestination(map,"consumerqueue") should beEqualTo(0)

    }


  }
}

case class HelloStringWithTime(val time : Long = System.currentTimeMillis()) extends CamelMessageSource {
  override def getMessage: CamelMessage = CamelMessage("Hello-"+time,Map())
}
