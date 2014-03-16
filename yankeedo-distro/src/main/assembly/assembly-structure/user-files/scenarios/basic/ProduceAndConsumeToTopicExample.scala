package org.greencheek.jms.yankeedo.app

import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.greencheek.jms.yankeedo.consumer.scenarioexecution.messageprocessor.SystemOutToStringCamelMessageProcessor

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
