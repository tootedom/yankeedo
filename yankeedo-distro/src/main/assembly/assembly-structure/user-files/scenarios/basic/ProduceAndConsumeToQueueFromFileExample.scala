package org.greencheek.jms.yankeedo.app

import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.SystemOutToStringCamelMessageProcessor
import org.greencheek.jms.yankeedo.scenarioexecution.producer.message.FileBasedMessageSource
import scala.reflect.io.File

/**
 * Created by dominictootell on 16/03/2014.
 */
class ProduceAndConsumeToQueueFromFileExample extends ScenarioContainer {
  val testDirectoryPath =  this.getClass.getResource("/ProduceAndConsumeToQueueFromFileExample").getPath

  withScenarios(
    List(
      createScenario(
        "Consumer messages scenario only consume half" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 50
          consume from queue "YankeedooProductAndConsumeToQueueExampleViaFile"
          prefetch 10
          with_message_consumer SystemOutToStringCamelMessageProcessor
      ),
      createScenario(
        "Product 100 messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 100
          produce to queue "YankeedooProductAndConsumeToQueueExampleViaFile"
          with_message_source new FileBasedMessageSource(testDirectoryPath,false,messageHeaders)
          with_persistent_delivery
      )
    )
  )

  def messageHeaders = Some((file : Option[File]) => {
      file match {
        case Some(f) => {
          Map( "FileName" -> f.name, "Last-Modified" -> f.lastModified,
            "SystemCurrentTimeInMillis" -> System.currentTimeMillis())
        }
        case None => {
          Map("SystemCurrentTimeInMillis" -> System.currentTimeMillis())
        }
      }
    })
}
