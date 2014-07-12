import java.util.concurrent.TimeUnit

import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.SystemOutToStringCamelMessageProcessor
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer

import scala.concurrent.duration._

class ProduceAndConsumeToQueueWithNoPersistenceExample extends ScenarioContainer {
  withScenarios(
    List(
      createScenario(
        "Consumer messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_consumed 20
          consume from queue "YankeedoProduceAndConsumeToQueueWithNoPersistenceExample"
          prefetch 1
          with_per_message_delay_of(Duration(200,TimeUnit.MILLISECONDS))
          with_message_consumer SystemOutToStringCamelMessageProcessor
      ),
      createScenario(
        "Product 100 messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100
          produce to queue "YankeedoProduceAndConsumeToQueueWithNoPersistenceExample"
          with_per_message_delay_of Duration(100,MILLISECONDS)
          with_no_persistent_delivery and
          with_message_ttl_of(Duration(5,TimeUnit.SECONDS))

      )
    )
  )
  outputStats()
  recordFirstMessageTiming(false)
  useNanoTiming(false)
}