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
package org.greencheek.jms.yankeedo.structure.dsl;

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import scala.concurrent.duration._

import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.greencheek.jms.yankeedo.structure.scenario.Scenario
import org.greencheek.jms.yankeedo.structure.actions.{Queue, DurableTopic, JmsConsumerAction, JmsProducerAction}
import org.greencheek.jms.yankeedo.scenarioexecution.producer.message.CamelMessageSource
import akka.camel.CamelMessage
import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.SystemOutToStringCamelMessageProcessor

/**
 * User: dominictootell
 * Date: 03/02/2013
 * Time: 21:05
 */
@RunWith(classOf[JUnitRunner])
class DslSpec extends Specification{

  "The DSL " should {
    "Test that connect_to can join a scenario name to a broker url " in {
      val scenarioWithNameAndUrl = "News Scenario 1" connect_to "localhost:61616"

      scenarioWithNameAndUrl.scenario.scenarioName must be equalTo "News Scenario 1"
      scenarioWithNameAndUrl.scenario.jmsurl must be equalTo Some("localhost:61616")

    }
    "When a scenario name, url and produce action are given, a Scenario can be created" in {
      val scenario = createScenario("ScenarioName" connect_to "localhost:11111"
                     produce to topic "dom")

      scenario must haveClass[Scenario]

      scenario.jmsAction must haveClass[JmsProducerAction]
    }
    "When a scenario name, url and consumer action are given, a Scenario can be created" in {
      val scenario = createScenario("ScenarioName" connect_to "localhost:11111"
        consume from topic "dom")

      scenario must haveClass[Scenario]

      scenario.jmsAction must haveClass[JmsConsumerAction]
    }
    "Be able to create a scenario that runs for a duration" in {
      val scenario = createScenario(
        "durable topic scenario" connect_to "localhost:61616"
          run_for Duration(11,SECONDS)
          until_no_of_messages_sent 101
          produce to queue "queue"
      )

      scenario.numberOfMessages must be equalTo 101
      scenario.runForDuration must be equalTo Duration(11,SECONDS)
      scenario.jmsAction must haveClass[JmsProducerAction]
    }
    "Be able to create a scenario that runs a number of actors" in {
      val scenario = createScenario(
        "any old scenario" connect_to "localhost:61616"
        with_no_of_actors 22
        consume from queue "tom"
      )

      scenario.numberOfActors must be equalTo(22)

      scenario.jmsAction must haveClass[JmsConsumerAction]

      scenario.jmsAction.destination must haveClass[Queue]
    }
    "when a durable topic is used a subscription name is required " in {
      val scenario = createScenario(
        "durable topic scenario" connect_to "localhost:61616"
        consume from durabletopic "durabletopic-xyz"
        with_subscription_name "bob"
      )

      scenario.jmsAction must haveClass[JmsConsumerAction]
      val jmsAction = scenario.jmsAction

      jmsAction.destination must haveClass[DurableTopic]

      jmsAction.destination.asInstanceOf[DurableTopic].subscriptionName must be equalTo "bob"
      jmsAction.destination.asInstanceOf[DurableTopic].name must be equalTo "durabletopic-xyz"
    }
    "When a consumer action is specified that a prefetch can be specified" in {
      val scenario = createScenario(
        "ScenarioName" connect_to "localhost:1111"
        consume from queue "dom"
        prefetch 10
      )

      scenario.jmsAction.asInstanceOf[JmsConsumerAction].prefetch must be equalTo 10
    }
    "When a consumer action is specified that a prefetch, number of consumers and message processor can be specified " in {
      var messageProcessor = SystemOutToStringCamelMessageProcessor

      var scenario = createScenario(
        "ScenarioName" connect_to "localhost:1111"
          consume from queue "dom"
          prefetch 10
          with_message_consumer SystemOutToStringCamelMessageProcessor
          number_of_consumers 10
      )

      var action = scenario.jmsAction.asInstanceOf[JmsConsumerAction]

      action.messageProcessor == messageProcessor
      action.numberOfConsumers must be equalTo 10
      action.prefetch must be equalTo 10

      scenario = createScenario(
        "ScenarioName" connect_to "localhost:1111"
          consume from queue "dom"
          with_message_consumer SystemOutToStringCamelMessageProcessor
          prefetch 10
          number_of_consumers 10
      )

      action = scenario.jmsAction.asInstanceOf[JmsConsumerAction]

      action.messageProcessor == messageProcessor
      action.numberOfConsumers must be equalTo 10
      action.prefetch must be equalTo 10

    }
    "when a producer action is specified, that delay between messages can be specified, and message processor can be set " in {
      val messageSource = new CamelMessageSource {
        def getMessage: CamelMessage = CamelMessage("xxx",Map.empty)
      }

      var scenario = createScenario(
        "ScenarioName" connect_to "localhost:1111"
          produce to queue "dom"
          with_message_source messageSource
      )

      var action = scenario.jmsAction.asInstanceOf[JmsProducerAction]

      action.messageSource == messageSource

      scenario = createScenario(
        "ScenarioName" connect_to "localhost:1111"
          produce to queue "dom"
          with_per_message_delay_of Duration(10,SECONDS)
          with_message_source messageSource

      )

      action = scenario.jmsAction.asInstanceOf[JmsProducerAction]

      action.delayBetweenMessages must be equalTo Duration(10,SECONDS)

      scenario = createScenario(
        "ScenarioName" connect_to "localhost:1111"
          produce to queue "dom"
          with_per_message_delay_of Duration(5,SECONDS)
          with_message_source messageSource
          with_broker_ack and
          with_persistent_delivery and
      )

      action = scenario.jmsAction.asInstanceOf[JmsProducerAction]

      action.delayBetweenMessages must be equalTo Duration(5,SECONDS)
      action.asyncSend must be equalTo false
      action.persistentDelivery must be equalTo true
      action.messageSource == messageSource

    }
    "When a producer action is specified, that persistent delivery and broker acks can be set " in {
      var scenario = createScenario(
        "ScenarioName" connect_to "localhost:1111"
        produce to queue "dom"
        with_broker_ack and
        with_persistent_delivery
      )

      var action = scenario.jmsAction.asInstanceOf[JmsProducerAction]

      action.asyncSend must be equalTo false
      action.persistentDelivery must be equalTo true

      scenario = createScenario(
        "ScenarioName" connect_to "localhost:1111"
          produce to queue "dom"
          with_persistent_delivery and
          with_broker_ack

      )

      action = scenario.jmsAction.asInstanceOf[JmsProducerAction]

      action.asyncSend must be equalTo false
      action.persistentDelivery must be equalTo true


      scenario = createScenario(
        "ScenarioName" connect_to "localhost:1111"
          produce to queue "dom"
          with_no_broker_ack and
          with_no_persistent_delivery
      )

      action = scenario.jmsAction.asInstanceOf[JmsProducerAction]

      action.asyncSend must be equalTo true
      action.persistentDelivery must be equalTo false

      scenario = createScenario(
        "ScenarioName" connect_to "localhost:1111"
          produce to queue "dom"
          with_no_persistent_delivery and
          with_no_broker_ack
      )

      action = scenario.jmsAction.asInstanceOf[JmsProducerAction]

      action.asyncSend must be equalTo true
      action.persistentDelivery must be equalTo false

      scenario = createScenario(
        "ScenarioName" connect_to "localhost:1111"
          produce to queue "dom"
          with_no_persistent_delivery and
          with_no_broker_ack
      )

      action = scenario.jmsAction.asInstanceOf[JmsProducerAction]

      action.asyncSend must be equalTo true
      action.persistentDelivery must be equalTo false

    }

  }
}
