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

import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import akka.actor.{Props, ActorRef}
import org.greencheek.jms.yankeedo.scenarioexecution.{StartExecutingScenarios, ReturnScenarioActorSystems, ScenarioActorSystems, ScenariosExecutionManager}
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Await
import java.util.concurrent.TimeUnit
import org.greencheek.jms.yankeedo.consumer.messageprocessor.CamelMessageProcessor
import akka.camel.CamelMessage
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

/**
 * Created by dominictootell on 15/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class TestMessageProcessorExceptionHandlingSpec extends BrokerBasedSpec {

  val myContext = WithActorSystem();


  "Producing messages" >> {
    "Check that message is not consumed when exception throw, but message processor says not to consume" in myContext {
      doTestConsumer(myContext,false,"queuewithnoconsume",100,1) should beTrue
    }

    "Check that message is consumed when exception throw, but message processor says to consume" in myContext {
      doTestConsumer(myContext,true,"queuewithconsume",99,1) should beTrue
    }

    def doTestConsumer(testContext : WithActorSystem, consumeOnException: Boolean, queueName : String, messagesExpectedOnQueue : Int,
                       messageExceptedToBeSentForProcessing : Int) : Boolean = {
      val appLatch = testContext.latch
      val actorSystem = testContext.actorSystem

      val messageProcessor = new MessageProcessorThrowingException(consumeOnException)

      val producerScenario1 = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_consumed 1
          consume from queue queueName
          with_message_consumer messageProcessor
          prefetch 1
      )

      val producerScenario2 = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100
          produce to queue queueName
          with_persistent_delivery
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
        ok = appLatch.await(15,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }

      Thread.sleep(5000)
      ok should beTrue

      val map = broker.getBroker.getDestinationMap()
      getMessageCountForQueueDestination(map,queueName) should beEqualTo(messagesExpectedOnQueue)
      messageProcessor.numberOfMessagesProcessed should beEqualTo(messageExceptedToBeSentForProcessing)

      true
    }
  }




  class MessageProcessorThrowingException(val consumeOnException : Boolean = true) extends CamelMessageProcessor{
    @volatile var _numberOfMessagesProcessed : Int = 0

    def process(message: CamelMessage) {
      _numberOfMessagesProcessed+=1
      throw new Exception("MessageProcessorThrowingException")
    }

    def consumerOnError: Boolean = consumeOnException

    def numberOfMessagesProcessed : Int = {
      _numberOfMessagesProcessed
    }
  }

}
