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
package org.greencheek.jms.yankeedo.app

import org.greencheek.jms.yankeedo.structure.scenario._
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import scala.concurrent.duration._
import org.greencheek.jms.util.CountingMessageProcessor
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.TimeUnit
import org.greencheek.jms.yankeedo.stats.DefaultOutputStats

/**
 * Created by dominictootell on 16/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class TestScenarioContainerExecutorSpec extends BrokerBasedSpec {

  "Producing messages" >> {
    "Check that the scenarios are executed" in {

      val messageProcessor = new CountingMessageProcessor()

      val consumerScenario1 = createScenario(
        "consumer message with a message processor" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          recordStatsImmediately false
          until_no_of_messages_consumed -1
          consume from queue "scenariocontainer"
          with_message_consumer messageProcessor
          prefetch 1
      )

      val producerScenario1 = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          recordStatsImmediately false
          until_no_of_messages_sent -1
          produce to queue "scenariocontainer"
          with_per_message_delay_of Duration(1,SECONDS)
          with_persistent_delivery
      )

      val scenarioContainer = ScenarioContainer(consumerScenario1,producerScenario1)
      scenarioContainer.runFor(Duration(6,SECONDS)).outputStatsOptions(Some(new DefaultOutputStats()))


      ScenarioContainerExecutor.executeScenarios(scenarioContainer) should beEqualTo(1)


      messageProcessor.numberOfPersistentMessages should greaterThan(4)
      messageProcessor.numberOfPersistentMessages should lessThan(10)

    }

    "Check that the scenarios are executed for a limited time" in {

      val messageProcessor = new CountingMessageProcessor()

      val consumerScenario1 = createScenario(
        "consume messages for global timeout" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_consumed -1
          consume from queue "scenariocontainer"
          with_message_consumer messageProcessor
          prefetch 1
      )

      val producerScenario1 = createScenario(
        "produce messages for at least 10 seconds" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent -1
          produce to queue "scenariocontainer"
          with_per_message_delay_of Duration(1,SECONDS)
          with_persistent_delivery
      )

      val scenarioContainer = ScenarioContainer(consumerScenario1,producerScenario1)
      scenarioContainer.runFor(Duration(10,SECONDS))


      ScenarioContainerExecutor.executeScenarios(scenarioContainer,Duration(5,SECONDS)) should beEqualTo(0)


      messageProcessor.numberOfPersistentMessages should greaterThanOrEqualTo(4)
      messageProcessor.numberOfPersistentMessages should lessThan(10)

      val nanos : TimeUnit = TimeUnit.NANOSECONDS


    }

    "Produce and consume,as fast as possible, for a limited time" in {

      val messageProcessor = new CountingMessageProcessor()

      val consumerScenario1 = createScenario(
        "consume messages as fast as possible" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          recordStatsImmediately false
          until_no_of_messages_consumed -1
          consume from queue "fastqueue"
          with_message_consumer messageProcessor
          prefetch 100
      )

      val producerScenario1 = createScenario(
        "produce messages,as fast as possible, for at least 10 seconds" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
          recordStatsImmediately false
          until_no_of_messages_sent -1
          produce to queue "fastqueue"
          with_persistent_delivery
      )

      val scenarioContainer = ScenarioContainer(consumerScenario1,producerScenario1)
      scenarioContainer.runFor(Duration(5,SECONDS)).outputStatsOptions(Some(new DefaultOutputStats()))

      ScenarioContainerExecutor.executeScenarios(scenarioContainer,Duration(5,SECONDS)) should beEqualTo(0)

      messageProcessor.numberOfPersistentMessages should greaterThan(10)

    }

  }
}