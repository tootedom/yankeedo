
import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.SystemOutToStringCamelMessageProcessor
import org.greencheek.jms.yankeedo.scenarioexecution.producer.message.{DirectoryBasedMessageSource, FileBasedMessageSource}
import scala.reflect.io.File

/**
 * Created by dominictootell on 16/03/2014.
 */
class ProductAndConsumeToQueueFromADirectory extends ScenarioContainer {
  val testDirectoryPath =  this.getClass.getResource("/sample-files").getPath

  val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
    filter = DirectoryBasedMessageSource.FILTER_BY_EXTENSION("json"),
    sortOrder = DirectoryBasedMessageSource.SORT_BY_NAME_CASE_SENSITIVE,
    sendFilesAsBytes = false, messageHeaders = Some( (file:File) => {
      Map( "FileName" -> file.name, "Last-Modified" -> file.lastModified)
    }))

  withScenarios(
    List(
      createScenario(
        "Consumer messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 4
          consume from queue "YankeedooProductAndConsumeToQueueFromDirectoryExample"
          prefetch 10
          with_message_consumer SystemOutToStringCamelMessageProcessor
      ),
      createScenario(
        "Product 100 messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 10
          produce to queue "YankeedooProductAndConsumeToQueueFromDirectoryExample"
          with_message_source directorySource
          with_persistent_delivery
      )
    )
  )

}