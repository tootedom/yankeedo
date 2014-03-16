package org.greencheek.jms.util

import akka.camel.CamelMessage
import org.greencheek.jms.yankeedo.consumer.messageprocessor.CamelMessageProcessor

/**
 * Created by dominictootell on 16/03/2014.
 */
class CountingMessageProcessor extends CamelMessageProcessor{
  @volatile var _numberOfPersistentMessages : Int = 0

  def process(message: CamelMessage) {
    val value = message.getHeaders.get("JMSDeliveryMode")
    if(value !=null) {
      if(value.equals(2)) {
        _numberOfPersistentMessages+=1
      }
    }
  }

  def consumerOnError: Boolean = true

  def numberOfPersistentMessages : Int = {
    _numberOfPersistentMessages
  }
}
