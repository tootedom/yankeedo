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

import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.{CamelMessageProcessor, SystemOutToStringCamelMessageProcessor}

import scala.concurrent.duration.Duration


/**
 * User: dominictootell
 * Date: 11/01/2013
 * Time: 09:02
 */
object JmsConsumerAction {
  val DEFULAT_NUMBER_OF_CONSUMERS = 1
  val DEFAULT_PREFETCH = 1
  val DEFAULT_PER_MESSAGE_PROCESSING_DELAY = Duration.Zero
  final def DEFAULT_MESSAGE_PROCESSOR = SystemOutToStringCamelMessageProcessor

}

class JmsConsumerAction(val destination: JmsDestination,
                        val numberOfConsumers: Int = JmsConsumerAction.DEFULAT_NUMBER_OF_CONSUMERS,
                        val prefetch: Int = JmsConsumerAction.DEFAULT_PREFETCH,
                        val messageProcessor: CamelMessageProcessor = JmsConsumerAction.DEFAULT_MESSAGE_PROCESSOR,
                        val messageDelay : Duration = JmsConsumerAction.DEFAULT_PER_MESSAGE_PROCESSING_DELAY) extends JmsAction {

  def consumeWithMessageProcessor(processor: CamelMessageProcessor) =
    new JmsConsumerAction(destination, numberOfConsumers, prefetch, processor,messageDelay)


  def withConcurrentConsumers(number: Int) = {
    new JmsConsumerAction(destination, number, prefetch, messageProcessor,messageDelay)
  }

  def withPrefetch(number: Int) = new JmsConsumerAction(destination, numberOfConsumers, number, messageProcessor,messageDelay)

  def processWithPerMessageDelayOf(delay : Duration) =  new JmsConsumerAction(destination, numberOfConsumers, prefetch, messageProcessor,delay)

  override def toString = {
    val buf = new StringBuilder

    buf ++= "consumer(" ++= "concurrent-consumers=" ++= numberOfConsumers.toString += ','
    buf ++= "prefetch=" ++= prefetch.toString += ','
    buf ++= "destination=" ++= destination.toString
    buf += ')'

    buf.toString()
  }
}
