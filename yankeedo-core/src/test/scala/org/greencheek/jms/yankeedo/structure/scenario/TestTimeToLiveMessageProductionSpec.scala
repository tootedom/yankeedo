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

import java.util.concurrent.TimeUnit

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import akka.actor.{ActorRef, Props}
import akka.camel.CamelMessage
import akka.pattern.ask
import akka.util.Timeout
import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.CamelMessageProcessor
import org.greencheek.jms.yankeedo.scenarioexecution.{ReturnScenarioActorSystems, ScenarioActorSystems, ScenariosExecutionManager, StartExecutingScenarios}
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.concurrent.Await
import scala.concurrent.duration._
/**
 * Created by dominictootell on 14/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class TestTimeToLiveMessageProductionSpec extends BrokerBasedSpec {

  val myContext = WithActorSystem()


  "Producing messages" >> {
    "Check that messages have been sent as non persistent" in myContext {
      val appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      System.out.println("==========")
      System.out.println("LATCH : " + appLatch)
      System.out.println("==========")
      System.out.flush()

      val producerScenario1 = createScenario(
        "produce 10 non persistent messages" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 10
          produce to queue "nonpersistentqueue"
          with_no_persistent_delivery and
          with_message_ttl_of(Duration(5,TimeUnit.SECONDS))
      )

      val producerScenario2 = createScenario(
        "produce 10 persistent messages" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 10
          produce to queue "nonpersistentqueue"
          with_persistent_delivery and
          with_message_ttl_of(Duration(5,TimeUnit.SECONDS))
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

      Thread.sleep(7000)

      ok should beTrue

      val map = broker.getBroker.getDestinationMap()
      getMessageCountForQueueDestination(map,"DLQ.nonpersistentqueue") should beEqualTo(10)
      getMessageCountForQueueDestination(map,"nonpersistentqueue") should beEqualTo(0)
    }


  }


}
