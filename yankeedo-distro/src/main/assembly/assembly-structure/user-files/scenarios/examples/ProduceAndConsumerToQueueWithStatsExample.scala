import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.SystemOutToStringCamelMessageProcessor
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer

class ProduceAndConsumeToQueueWithStatsExample extends ScenarioContainer {
  withScenarios(
    List(
      createScenario(
        "Consumer messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100
          consume from queue "YankeedooProductAndConsumeToQueueExample"
          prefetch 10
          with_message_consumer SystemOutToStringCamelMessageProcessor
      ),
      createScenario(
        "Product 100 messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100
          produce to queue "YankeedooProductAndConsumeToQueueExample"
          with_persistent_delivery
      )
    )
  )
  outputStatus()
}