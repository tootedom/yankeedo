import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.SystemOutToStringCamelMessageProcessor
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer
import scala.concurrent.duration._

class ProduceAndConsumeToQueueWithStatsExample extends ScenarioContainer {
  withScenarios(
    List(
      createScenario(
        "Consumer messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_consumed 100000
          consume from queue "YankeedooProductAndConsumeToQueueExample"
          prefetch 1
          with_message_consumer SystemOutToStringCamelMessageProcessor
      ),
      createScenario(
        "Produce 100000 messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100000
          produce to queue "YankeedooProductAndConsumeToQueueExample"
          with_persistent_delivery
      )
    )
  )
  outputStats()
  recordFirstMessageTiming(false)
  useNanoTiming(false)
}