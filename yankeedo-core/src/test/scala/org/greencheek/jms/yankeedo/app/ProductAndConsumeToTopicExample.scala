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

import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.SystemOutToStringCamelMessageProcessor

/**
 * Created by dominictootell on 16/03/2014.
 */
class ProduceAndConsumeToTopicExample extends ScenarioContainer {
  withScenarios(
    List(
      createScenario(
        "Consumer messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100
          consume from topic "YankeedooProductAndConsumeToTopicExample"
          prefetch 10
          with_message_consumer SystemOutToStringCamelMessageProcessor
      ),
      createScenario(
        "Product 100 messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100
          produce to topic "YankeedooProductAndConsumeToTopicExample"
          with_persistent_delivery
      )
    )
  )

}
