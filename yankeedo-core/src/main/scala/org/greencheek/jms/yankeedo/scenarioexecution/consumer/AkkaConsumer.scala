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

import akka.camel.{CamelMessage, Consumer}
import akka.actor.Status.Failure
import akka.camel.Ack
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import org.greencheek.jms.yankeedo.structure.actions.{JmsConsumerAction => JmsCons, DurableTopic, Queue, Topic}
import org.greencheek.jms.yankeedo.scenarioexecution.ConsumerFinished
import org.greencheek.jms.yankeedo.consumer.messageprocessor.CamelMessageProcessor
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.duration.SECONDS

/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 16:32
 */
class AkkaConsumer(val jmsAction : JmsCons,
                   val messageProcessor : CamelMessageProcessor,
                   val messagesAttemptedToProcess : AtomicLong
                   ) extends Consumer {
  val messagesRecieved = new AtomicLong(0);
  val endpoint  = {
    jmsAction destination match {
      case Topic(destName) => "jms:topic:" + destName + "?concurrentConsumers=" + jmsAction.numberOfConsumers
      case Queue(destName) => "jms:queue:" + destName + "?concurrentConsumers=" + jmsAction.numberOfConsumers
      case DurableTopic(destName,subscriptionName,clientId) => {
        val buf = new StringBuilder(125)
        buf.append("jms:topic:").append(destName).append("?concurrentConsumers=")
        buf.append(jmsAction.numberOfConsumers)
//        buf.append("&clientId=").append(clientId)
        buf.append("&durableSubscriptionName=").append(subscriptionName).toString
      }

    }
  }
  val infinite : Boolean = if(messagesAttemptedToProcess.get() == -1) true else false
  val stopped = new AtomicBoolean(false)

  override def autoAck = false
  override def endpointUri = endpoint;
  override def replyTimeout: FiniteDuration = Duration(1,SECONDS)

  private def stop = {
    try {
      // if we stop the actor (consumer), then camel
      // still has the message, and waits a while to stop.
      // so we just mark the consumer via the stopped.  And fail any incoming messages
//      context.stop(self)
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
      // if this is ==0 the message consumer (this/self) will send a stop message
      // if this is <0 the message then it will just not ack the message, and will send a failure
      if (stopped.get) {
        sender ! Failure(new Throwable("Message Consumer Finished.  Not Consuming message, waiting for shutdown"))
      }

      val sharedMessageNumber =  { if (infinite) 1 else messagesAttemptedToProcess.decrementAndGet() }

      messagesRecieved.incrementAndGet()

      if(sharedMessageNumber<0) {
        markStopped
        sender ! Failure(new Throwable("Message Consumer Finished.  Not Consuming message, waiting for shutdown"))
      } else {

        var consumeMessage = true
        var exception : Option[Throwable] = None
        try {
          messageProcessor.process(msg)
        } catch {
          case t : Throwable => {
            exception = Some(t)
            // nothing can be done.  Going to consume the message and log.
            // problem with messageProcessor.
            if (messageProcessor.consumerOnError) {
              consumeMessage = true
            } else {
              consumeMessage = false
            }
          }
        }



        if (consumeMessage) {
          System.out.println("ACKKKKKK")
          sender ! Ack
        }
        else {
          sender ! Failure(exception.get)
        }

        if (sharedMessageNumber <= 0) {
          stop
        }
      }
    }
    case _ => {
      messagesRecieved.incrementAndGet()
    }
  }

}


