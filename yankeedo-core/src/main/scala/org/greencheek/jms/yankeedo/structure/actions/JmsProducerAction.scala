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
package org.greencheek.jms.yankeedo.structure.actions

import org.greencheek.jms.yankeedo.scenarioexecution.producer.message.{ CamelMessageSource }
import concurrent.duration.Duration
import akka.camel.CamelMessage
import java.util.UUID

/**
 * User: dominictootell
 * Date: 12/01/2013
 * Time: 09:04
 */
object JmsProducerAction {
  val DEFAULT_PERSISTENT_DELIVERY = true
  val DEFAULT_ASYNC_SEND = true
  val DEFAULT_DELAY_BETWEEN_MESSAGES = Duration.MinusInf
  val DEFAULT_MESSAGE_TIME_TO_LIVE = Duration.MinusInf
  val DEFAULT_MESSAGE_SOURCE = new CamelMessageSource {
    def getMessage: CamelMessage = CamelMessage(UUID.randomUUID(),Map.empty)
  }

}

class JmsProducerAction(val destination : JmsDestination,
                        val messageSource : CamelMessageSource = JmsProducerAction.DEFAULT_MESSAGE_SOURCE,
                        val persistentDelivery : Boolean = JmsProducerAction.DEFAULT_PERSISTENT_DELIVERY,
                        val asyncSend : Boolean = JmsProducerAction.DEFAULT_ASYNC_SEND,
                        val delayBetweenMessages : Duration = JmsProducerAction.DEFAULT_DELAY_BETWEEN_MESSAGES,
                        val timeToLive : Duration = JmsProducerAction.DEFAULT_MESSAGE_TIME_TO_LIVE
                        ) extends JmsAction {


  def withMessageSource(messageSource : CamelMessageSource) = new JmsProducerAction(destination,messageSource,persistentDelivery,asyncSend,delayBetweenMessages,timeToLive)
  def withPersistentDelivery(persistent : Boolean) = new JmsProducerAction(destination,messageSource,persistent,asyncSend,delayBetweenMessages,timeToLive)
  def withAsyncSend(async : Boolean) = new JmsProducerAction(destination,messageSource,persistentDelivery,async,delayBetweenMessages,timeToLive)
  def sendMessageWithDelayOf(delay : Duration) = new JmsProducerAction(destination,messageSource,persistentDelivery,asyncSend,delay,timeToLive)
  def sendMessageWithTTL(timeToLive : Duration) = new JmsProducerAction(destination,messageSource,persistentDelivery,asyncSend,delayBetweenMessages,timeToLive)


  override def toString = {
    val buf = new StringBuilder

    buf ++= "producer("
    buf ++= "persistent-delivery=" ++= persistentDelivery.toString += ','
    buf ++= "async-sends=" ++= asyncSend.toString  += ','
    buf ++= "destination=" ++= destination.toString += ','
    buf ++= "delayBetweenMessagesSends=" ++= delayBetweenMessages.toString += ','
    buf ++= "timeToLive" ++= timeToLive.toString
    buf += ')'

    buf.toString()
  }
}
