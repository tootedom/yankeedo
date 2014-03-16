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
import akka.pattern.ask
import scala.concurrent.Await
import java.util.concurrent.{CountDownLatch, TimeUnit}
import org.greencheek.jms.yankeedo.consumer.messageprocessor.CamelMessageProcessor
import akka.camel.CamelMessage
import org.apache.activemq.broker.region.{Subscription, DestinationStatistics}
import org.apache.activemq.command.ActiveMQDestination

/**
 * Created by dominictootell on 15/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class TestDurableTopicScenarioSpec extends BrokerBasedSpec {

  val myContext = WithActorSystem();


  "Producing messages" >> {
    "Check messages sent to a durable topic persists" in myContext {

      var appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      val messageProcessor = new CountingMessageProcessor

      val producerScenario1 = createScenario(
        "Consumer 1 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_consumed 10
          consume from durabletopic "persistenttopic"
          with_subscription_name_and_clientid("bob","bob")
          with_message_consumer messageProcessor
          prefetch 1
      )

      val producerScenario2 = createScenario(
        "Product 100 messages scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100
          produce to topic "persistenttopic"
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

      ok should beTrue

      messageProcessor.numberOfMessagesProcessed should beEqualTo(10)

      val scenario = createScenario(
        "Consumer 1 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_consumed 15
          consume from durabletopic "persistenttopic"
          with_subscription_name_and_clientid("bob","bob")
          with_message_consumer messageProcessor
          prefetch 1
      )

      appLatch = new CountDownLatch(1)

      val scenarioExecutor2 : ActorRef = actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,ScenarioContainer(scenario))))
      scenarioExecutor2 ! StartExecutingScenarios


      ok = false
      try {
        ok = appLatch.await(15,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }

      ok should beTrue


      messageProcessor.numberOfMessagesProcessed should beEqualTo(25)

      val map = broker.getBroker.getDestinationMap()

      getMessageCountForTopicDestinationCustomCounter(map,"persistenttopic",
      { stat : DestinationStatistics => stat.getEnqueues.getCount-stat.getDispatched.getCount}) should be lessThanOrEqualTo(75)

      val subscription : Option[List[Subscription]] = getConsumersForDestination(map,"persistenttopic",{dest : ActiveMQDestination => dest.isTopic})

      subscription.isDefined should beTrue

      val subscriptionList : List[Subscription] = subscription.get

      subscriptionList.size should beEqualTo(1)

      subscriptionList(0).getDequeueCounter should beEqualTo(25)

    }

    "Check messages sent to a durable topic persists with default client id" in myContext {

      var appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      val messageProcessor = new CountingMessageProcessor

      val producerScenario1 = createScenario(
        "Consumer 1 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_consumed 10
          consume from durabletopic "persistenttopic2"
          with_subscription_name "subscriber2"
          with_message_consumer messageProcessor
          prefetch 1
      )

      val producerScenario2 = createScenario(
        "Product 100 messages scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100
          produce to topic "persistenttopic2"
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
      ok should beTrue
      messageProcessor.numberOfMessagesProcessed should beEqualTo(10)
      val map = broker.getBroker.getDestinationMap()

      val subscription : Option[List[Subscription]] = getConsumersForDestination(map,"persistenttopic2",{dest : ActiveMQDestination => dest.isTopic})

      subscription.isDefined should beTrue

      val subscriptionList : List[Subscription] = subscription.get

      subscriptionList.size should beEqualTo(1)

      subscriptionList(0).getPrefetchSize should beEqualTo(1)
      subscriptionList(0).getConsumerInfo.getSubscriptionName must beEqualTo("subscriber2")
      subscriptionList(0).getDequeueCounter should beEqualTo(10)
    }

    "Check messages sent to a durable topic persists with default subscriber name" in myContext {

      var appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      val messageProcessor = new CountingMessageProcessor

      val producerScenario1 = createScenario(
        "Consumer 1 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_consumed 33
          consume from durabletopic "persistenttopic3"
          with_clientid "clientidentifier"
          with_message_consumer messageProcessor
          prefetch 1
      )

      val producerScenario2 = createScenario(
        "Product 100 messages scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100
          produce to topic "persistenttopic3"
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
      ok should beTrue
      messageProcessor.numberOfMessagesProcessed should beEqualTo(33)
      val map = broker.getBroker.getDestinationMap()

      val subscription : Option[List[Subscription]] = getConsumersForDestination(map,"persistenttopic3",{dest : ActiveMQDestination => dest.isTopic})

      subscription.isDefined should beTrue

      val subscriptionList : List[Subscription] = subscription.get

      subscriptionList.size should beEqualTo(1)

      subscriptionList(0).getPrefetchSize should beEqualTo(1)
      subscriptionList(0).getConsumerInfo.getClientId must beEqualTo("clientidentifier")
      subscriptionList(0).getDequeueCounter should beEqualTo(33)
    }

    "Check messages sent to a durable topic persists with default information" in myContext {

      var appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      val messageProcessor = new CountingMessageProcessor

      val producerScenario1 = createScenario(
        "Consumer 1 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_consumed 55
          consume from durabletopic "persistenttopic4"
          with_default_subscriber_info()
          with_message_consumer messageProcessor
          prefetch 1
      )

      val producerScenario2 = createScenario(
        "Product 100 messages scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100
          produce to topic "persistenttopic4"
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
      ok should beTrue
      messageProcessor.numberOfMessagesProcessed should beEqualTo(55)
      val map = broker.getBroker.getDestinationMap()

      val subscription : Option[List[Subscription]] = getConsumersForDestination(map,"persistenttopic4",{dest : ActiveMQDestination => dest.isTopic})

      subscription.isDefined should beTrue

      val subscriptionList : List[Subscription] = subscription.get

      subscriptionList.size should beEqualTo(1)

      subscriptionList(0).getPrefetchSize should beEqualTo(1)
      subscriptionList(0).getConsumerInfo.getClientId must contain("yankeedoo.client")
      subscriptionList(0).getConsumerInfo.getSubscriptionName must contain("yankeedoo.subscription")
      subscriptionList(0).getDequeueCounter should beEqualTo(55)
    }
  }

  class CountingMessageProcessor extends CamelMessageProcessor {
    @volatile var _numberOfMessagesProcessed : Int = 0

    def process(message: CamelMessage) {
      _numberOfMessagesProcessed+=1
    }

    def consumerOnError: Boolean = true

    def numberOfMessagesProcessed : Int = {
      _numberOfMessagesProcessed
    }
  }
}
