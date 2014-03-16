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

import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import akka.actor.{Props, ActorRef}
import org.greencheek.jms.yankeedo.scenarioexecution.{ReturnScenarioActorSystems, StartExecutingScenarios, ScenarioActorSystems, ScenariosExecutionManager}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import java.util.concurrent.TimeUnit
import org.greencheek.jms.yankeedo.consumer.messageprocessor.CamelMessageProcessor
import akka.camel.CamelMessage
import akka.pattern.ask

/**
 * Created by dominictootell on 16/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class TestScenarioContainerRunForDurationSpec extends BrokerBasedSpec {

  val myContext = WithActorSystem();


  "Producing messages" >> {
    "Check that messages are sent up until the run for duration on the scenario container" in myContext {
      val appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      val messageProcessor = new MessagePersistentChecker()

      val consumerScenario1 = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_consumed -1
          consume from queue "scenariocontainer"
          with_message_consumer messageProcessor
          prefetch 1
      )

      val producerScenario1 = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent -1
          produce to queue "scenariocontainer"
          with_per_message_delay_of Duration(1,SECONDS)
          with_persistent_delivery
      )

      val scenarioContainer = ScenarioContainer(consumerScenario1,producerScenario1)
      scenarioContainer.runFor(Duration(5,SECONDS))

      val scenarioExecutor : ActorRef = actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,scenarioContainer)))
      scenarioExecutor ! StartExecutingScenarios

      implicit val timeout = Timeout(2,SECONDS)
      val future = scenarioExecutor ? ReturnScenarioActorSystems
      val result = Await.result(future, timeout.duration).asInstanceOf[ScenarioActorSystems]

      result should not beNull
      val actorSystemSize = result.actorSystems.size
      actorSystemSize should beEqualTo(2)

      var ok : Boolean = false
      try {
        ok = appLatch.await(15,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }

      ok should beTrue

      val map = broker.getBroker.getDestinationMap()
      messageProcessor.numberOfPersistentMessages should greaterThan(4)
      messageProcessor.numberOfPersistentMessages should lessThan(10)
    }


  }

  class MessagePersistentChecker extends CamelMessageProcessor{
    @volatile var _numberOfPersistentMessages : Int = 0

    def process(message: CamelMessage) {
      val value = message.getHeaders.get("JMSDeliveryMode")
      if(value !=null) {
        if(value.equals(2)) {
          _numberOfPersistentMessages+=1
        }
      }
    }

    def consumerOnError: Boolean = true

    def numberOfPersistentMessages : Int = {
      _numberOfPersistentMessages
    }
  }

}
