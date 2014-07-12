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
package org.greencheek.jms.yankeedo.scenarioexecution.consumer

import java.util.concurrent.locks.LockSupport

import akka.camel.{CamelMessage, Consumer}
import akka.actor.Status.Failure
import akka.camel.Ack
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import org.greencheek.jms.yankeedo.structure.actions.{JmsConsumerAction => JmsCons, DurableTopic, Queue, Topic}
import org.greencheek.jms.yankeedo.scenarioexecution.ConsumerFinished
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.duration.SECONDS
import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.CamelMessageProcessor
import org.LatencyUtils.LatencyStats
import grizzled.slf4j.Logging
import org.greencheek.jms.yankeedo.stats.TimingServices
import javax.jms.JMSException

/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 16:32
 */
class AkkaConsumer(val jmsAction : JmsCons,
                   val messageProcessor : CamelMessageProcessor,
                   val messagesAttemptedToProcess : AtomicLong,
                   val statsRecorder : TimingServices) extends Consumer with Logging {
  val messagesRecieved = new AtomicLong(0);
  val endpoint  = {
    jmsAction destination match {
      case Topic(destName) => "jms:topic:" + destName + "?concurrentConsumers=" + jmsAction.numberOfConsumers
      case Queue(destName) => "jms:queue:" + destName + "?concurrentConsumers=" + jmsAction.numberOfConsumers
      case DurableTopic(destName,subscriptionName,clientId) => {
        val buf = new StringBuilder(125)
        buf.append("jms:topic:").append(destName).append("?concurrentConsumers=")
        buf.append(jmsAction.numberOfConsumers)
        buf.append("&durableSubscriptionName=").append(subscriptionName).toString
      }

    }
  }
  val infinite : Boolean = if(messagesAttemptedToProcess.get() == -1) true else false
  val stopped = new AtomicBoolean(false)

  override def autoAck = false
  override def endpointUri = endpoint;
  override def replyTimeout: FiniteDuration = Duration(5,SECONDS)

  private def stop = {
    try {
      // if we stop the actor (consumer), then camel
      // still has the message, and waits a while to stop.
      // so we just mark the consumer via the stopped.  And fail any incoming messages
      if(!stopped.get()) {
        markStopped
        context.parent ! ConsumerFinished
      }
    } finally {
    }
  }

  private def markStopped {
    stopped.set(true)
  }

  def receive = {
    case msg: CamelMessage => {
      var isStopped : Boolean = false
      try {
        // if this is ==0 the message consumer (this/self) will send a stop message
        // if this is <0 the message then it will just not ack the message, and will send a failure
        if (stopped.get) {
          isStopped = true
          sender ! Failure(new JMSException("Message Consumer Has Been Stopped.  Not Consuming message, waiting for shutdown"))
        }
        else
        {

          val sharedMessageNumber = {
            if (infinite) 1 else messagesAttemptedToProcess.decrementAndGet()
          }

          messagesRecieved.incrementAndGet()

          if (sharedMessageNumber < 0) {
            isStopped = true
            markStopped
            sender ! Failure(new JMSException("Message Consumer Finished.  Not Consuming message, waiting for shutdown"))
          }
          else
          {
            var consumeMessage = true
            var exception: Option[Throwable] = None
            try {
              messageProcessor.process(msg)
            } catch {
              case t: Throwable => {
                exception = Some(t)
                warn("Exception calling message processor",t)
                // nothing can be done.  Going to consume the message and log.
                // problem with messageProcessor.
                if (messageProcessor.consumerOnError) {
                  consumeMessage = true
                } else {
                  consumeMessage = false
                }
              }
            } finally {
              if(jmsAction.messageDelay.gt(Duration.Zero)) {
                LockSupport.parkNanos(jmsAction.messageDelay.toNanos)
              }

              if (consumeMessage) {
                sender ! Ack
              }
              else {
                sender ! Failure(exception.get)
              }

              if (sharedMessageNumber == 0) {
                info("Max number of messages to processed has been reached")
                stop
              }
            }
          }
        }
      } finally {
        if(!isStopped) {
          statsRecorder.recordStats()
        }
      }

    }
  }

}


