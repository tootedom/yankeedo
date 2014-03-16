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
package org.greencheek.jms.yankeedo.structure.dsl

import org.greencheek.jms.yankeedo.structure.scenario.ScenarioBuilder.ScenarioBuilder
import org.greencheek.jms.yankeedo.structure.scenario.{ScenarioBuilder => Builder, Scenario}
import org.greencheek.jms.yankeedo.structure.RequiredParameter._
import scala.concurrent.duration._
import org.greencheek.jms.yankeedo.structure.actions._
import org.greencheek.jms.yankeedo.scenarioexecution.producer.message.CamelMessageSource
import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.CamelMessageProcessor

/**
 * User: dominictootell
 * Date: 02/02/2013
 * Time: 16:57
 */
object Dsl {

  object and
  object from
  object to

  type ScenarioWithoutAction = ScenarioBuilder[TRUE, FALSE, TRUE]

  trait NamedScenarioWithUrl {
    def scenario: ScenarioWithoutAction
  }

  trait ActionPopulatedScenario {
    val action: JmsAction
    val scenario: ScenarioWithoutAction
  }

  class ProducerPopulatedScenario(val action: JmsProducerAction, val scenario: ScenarioWithoutAction) extends ActionPopulatedScenario

  class ConsumerPopulatedScenario(val action: JmsConsumerAction, val scenario: ScenarioWithoutAction) extends ActionPopulatedScenario

  type ScenarioWithUrlAndName = NamedScenarioWithUrl

  type ProducerPopulatedActionBasedScenario = (to.type, ScenarioWithUrlAndName)
  type ConsumerPopulatedActionBasedScenario = (from.type , ScenarioWithUrlAndName)

  type DurableTopicScenario = (String, ConsumerPopulatedActionBasedScenario)


  class ScenarioCreator(name: String) {
    def connect_to(url: String): ScenarioWithUrlAndName = new ScenarioWithUrlAndName {
      def scenario = Builder.builder(name).withConnectionUrl(url)
    }
  }

  class MessageSent(namedScenario: ScenarioWithUrlAndName) {
    def until_no_of_messages_sent(number: Int): ScenarioWithUrlAndName = new NamedScenarioWithUrl {
      def scenario = namedScenario.scenario.runForMessages(number)
    }
    def until_no_of_messages_consumed(number: Int): ScenarioWithUrlAndName = new NamedScenarioWithUrl {
      def scenario = namedScenario.scenario.runForMessages(number)
    }
    def with_no_of_actors(number : Int ) : ScenarioWithUrlAndName = new NamedScenarioWithUrl {
      def scenario: Dsl.ScenarioWithoutAction = namedScenario.scenario.runWithConcurrency(number)
    }
    def run_for(time : Duration) :  ScenarioWithUrlAndName = new NamedScenarioWithUrl {
      def scenario: Dsl.ScenarioWithoutAction = namedScenario.scenario.runForDuration(time)
    }
  }

  class JmsActionCreator(scenario: ScenarioWithUrlAndName) {
    def consume(action: from.type): ConsumerPopulatedActionBasedScenario = (action, scenario)

    def produce(action: to.type): ProducerPopulatedActionBasedScenario = (action, scenario)
  }


  class JmsConsumerCreator(action: ConsumerPopulatedActionBasedScenario) {
    def topic(destination: String) = {
      createTopic(destination, action)
    }

    def queue(destination: String) = {
      createQueue(destination, action)
    }

    def durabletopic(destination: String) = {
      (destination, action)
    }


    private def createTopic(dest: String, action: ConsumerPopulatedActionBasedScenario) = {
      new ConsumerPopulatedScenario(
        JmsActionTypeBuilder.builder.consumeTopic(dest).build().asInstanceOf[JmsConsumerAction],
        action._2.scenario
      )
    }

    private def createQueue(dest: String, action: ConsumerPopulatedActionBasedScenario) = {
      new ConsumerPopulatedScenario(
        JmsActionTypeBuilder.builder.consumeQueue(dest).build().asInstanceOf[JmsConsumerAction],
        action._2.scenario
      )
    }
  }

  class JmsProducerCreator(action: ProducerPopulatedActionBasedScenario) {
    def topic(destination: String) = {
      createTopic(destination, action)
    }

    def queue(destination: String) = {
      createQueue(destination, action)
    }

    private def createTopic(dest: String, action: ProducerPopulatedActionBasedScenario) = {
      new ProducerPopulatedScenario(
        JmsActionTypeBuilder.builder.sendToTopic(dest).build().asInstanceOf[JmsProducerAction],
        action._2.scenario
      )
    }

    private def createQueue(dest: String, action: ProducerPopulatedActionBasedScenario) = {
      new ProducerPopulatedScenario(
        JmsActionTypeBuilder.builder.sendToQueue(dest).build().asInstanceOf[JmsProducerAction],
        action._2.scenario
      )
    }
  }

  class DurableTopicScenarioWithSubscriptionNameCreator(consumerScenario: DurableTopicScenario) {
    def with_subscription_name(subscriptionName: String ) = {
      val builder = consumerScenario._2._2.scenario
      new ConsumerPopulatedScenario(
        JmsActionTypeBuilder.builder.consumerDurableTopicWithSubscriptionName(consumerScenario._1, subscriptionName).build().asInstanceOf[JmsConsumerAction],
        builder
      )
    }

    def with_subscription_name_and_clientid(subscriptionName: String , clientId : String) = {
      val builder = consumerScenario._2._2.scenario
      new ConsumerPopulatedScenario(
        JmsActionTypeBuilder.builder.consumerDurableTopicWithSubscriptionAndClientId(consumerScenario._1, subscriptionName, clientId).build().asInstanceOf[JmsConsumerAction],
        builder
      )
    }

    def with_clientid( clientId : String) = {
      val builder = consumerScenario._2._2.scenario
      new ConsumerPopulatedScenario(
        JmsActionTypeBuilder.builder.consumerDurableTopicWithClientId(consumerScenario._1, clientId).build().asInstanceOf[JmsConsumerAction],
        builder
      )
    }

    def with_default_subscriber_info() = {
      val builder = consumerScenario._2._2.scenario
      new ConsumerPopulatedScenario(
        JmsActionTypeBuilder.builder.consumerDurableTopic(consumerScenario._1).build().asInstanceOf[JmsConsumerAction],
        builder
      )
    }
  }

  class ConsumerPopulatedScenarioConfigurer(consumerScenario: ConsumerPopulatedScenario) {
    def prefetch(prefetch: Int) = {
      new ConsumerPopulatedScenario(
        consumerScenario.action.withPrefetch(prefetch),
        consumerScenario.scenario
      )
    }

    def number_of_consumers(number: Int) = {
      new ConsumerPopulatedScenario(
        consumerScenario.action.withConcurrentConsumers(number),
        consumerScenario.scenario
      )
    }

    def with_message_consumer(consumer: CamelMessageProcessor) = {
      new ConsumerPopulatedScenario(
        consumerScenario.action.consumeWithMessageProcessor(consumer),
        consumerScenario.scenario
      )
    }
  }

  class ProducerPopulatedScenarioConfigurer(producerScenario: ProducerPopulatedScenario) {

    def with_broker_ack(chain: and.type): ProducerPopulatedScenario = {
      with_broker_ack()
    }

    def with_broker_ack() = {
      new ProducerPopulatedScenario(
        producerScenario.action.withAsyncSend(false),
        producerScenario.scenario
      )
    }

    def with_no_broker_ack(chain: and.type): ProducerPopulatedScenario = {
      with_no_broker_ack()
    }

    def with_no_broker_ack() = {
      new ProducerPopulatedScenario(
        producerScenario.action.withAsyncSend(true),
        producerScenario.scenario
      )
    }

    def with_no_persistent_delivery(chain: and.type): ProducerPopulatedScenario = {
      with_no_persistent_delivery()
    }

    def with_no_persistent_delivery() = {
      new ProducerPopulatedScenario(
        producerScenario.action.withPersistentDelivery(false),
        producerScenario.scenario
      )
    }

    def with_persistent_delivery(chain: and.type): ProducerPopulatedScenario = {
      with_persistent_delivery()
    }

    def with_persistent_delivery() = {
      new ProducerPopulatedScenario(
        producerScenario.action.withPersistentDelivery(true),
        producerScenario.scenario
      )
    }

    def with_message_source(messageSource: CamelMessageSource) = {
      new ProducerPopulatedScenario(
        producerScenario.action.withMessageSource(messageSource),
        producerScenario.scenario
      )
    }

//    def with_per_message_delay_of(delay: Duration, chain : and.type) = with_per_message_delay_of(delay)
    def with_per_message_delay_of(delay: Duration) = {
      new ProducerPopulatedScenario(
        producerScenario.action.sendMessageWithDelayOf(delay),
        producerScenario.scenario
      )
    }

  }


  def createScenario(scenario: ActionPopulatedScenario): Scenario = {
    scenario.scenario.withJmsAction(scenario.action).build()
  }


  implicit def NamedScenarioWithUrlToMessageDuration(scenario: ScenarioWithUrlAndName) = new MessageSent(scenario)

//  implicit def NamedScenarioWithUrlToDurationPassed(scenario: ScenarioWithUrlAndName) = new DurationPassed(scenario)

  implicit def StringToScenarioCreator(name: String) =
    new ScenarioCreator(name)


  implicit def ActionTypeToJmsActionCreator(scenario: ScenarioWithUrlAndName) =
    new JmsActionCreator(scenario)

  implicit def JmsActionToDestinationSpecifiedScenario(scenario: ConsumerPopulatedActionBasedScenario) =
    new JmsConsumerCreator(scenario)

  implicit def JmsActionToDestinationSpecifiedScenario(scenario: ProducerPopulatedActionBasedScenario) =
    new JmsProducerCreator(scenario)

  implicit def DurableTopicScenarioWithSubscriptionNameCreator(scenario: DurableTopicScenario) =
    new DurableTopicScenarioWithSubscriptionNameCreator(scenario)

  implicit def ProducerPopulatedScenarioToProducerPopulatedScenarioConfigurer(scenario: ProducerPopulatedScenario) =
    new ProducerPopulatedScenarioConfigurer(scenario)



  implicit def ConsumerPopulatedScenarioToConsumerPopulatedScenarioConfigurer(scenario: ConsumerPopulatedScenario) =
    new ConsumerPopulatedScenarioConfigurer(scenario)


//  def main(args: Array[String]) {
//
//    println("================")
//
//    println("1)")
//    println(createScenario(
//      "News Scenario 1"
//        connect_to "localhost:9999"
//        until_no_of_messages_sent 10
//        run_for (10 seconds)
//        produce to
//        topic "dom"
//        with_per_message_delay_of (10 seconds)
//        with_persistent_delivery and
//        with_broker_ack
//    )
//
//
//    )
//
//    println("2)")
//    println(createScenario("News Scenario 2"
//      connect_to "localhost:9999"
//      run_for (10 seconds)
//      until_no_of_messages_sent 10
//      consume from
//      durabletopic "tom" with_subscription_name "jjjj"
//      prefetch 10
//      number_of_consumers 10
//
//    ).toString()
//
//    )
//
//    println("3)")
//    println(createScenario(
//      "News Scenario 3"
//        connect_to "localhost:9999"
//        run_for (10 seconds)
//        produce to
//        topic "dom"
//
//    )
//
//    )
//
//    println("4)")
//    println(createScenario("News Scenario 4"
//      connect_to "localhost:9999"
//      produce to
//      queue "dom"
//
//    )
//    )
//
//  }

}
