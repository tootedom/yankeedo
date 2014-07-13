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
package org.greencheek.jms.yankeedo.scenarioexecution.producer

import akka.camel.CamelMessage
import org.greencheek.jms.yankeedo.scenarioexecution.producer.message.CamelMessageSource
import org.greencheek.jms.yankeedo.scenarioexecution.{ProducerFinished, ScenarioExecutionMonitor}
import org.greencheek.jms.yankeedo.structure.scenario.Scenario
import akka.actor.{ActorRef, ActorContext}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import akka.routing.RoundRobinGroup
import collection.JavaConversions
import java.util.{ Map ⇒ JMap, HashMap => JHashMap }
import org.greencheek.jms.yankeedo.structure.actions.{JmsProducerAction => Producer}

import scala.concurrent.duration.Duration


object ProducerMessageRouter {
  val PERSISTENT = 2
  val NON_PERSISTENT = 1
  val JMS_HEADER_DELIVERY_MODE = "JMSDeliveryMode"
  val JMS_HEADER_TTL = "JMSExpiration"
}
/**
 * User: dominictootell
 * Date: 12/01/2013
 * Time: 19:39
 */
class ProducerMessageRouter(scenario : Scenario,
                            children : (ActorContext, AtomicLong) => List[ActorRef]) extends ScenarioExecutionMonitor(scenario,children) {

  import ProducerMessageRouter._
  var messagesToSend = scenario.numberOfMessages
  val infinite : Boolean = if(messagesToSend == -1) true else false
  val stopped = new AtomicBoolean(false)
  val producer : Producer =  scenario.jmsAction.asInstanceOf[Producer]
  val messageSource : CamelMessageSource = producer.messageSource


  val router = context.actorOf(new RoundRobinGroup(JavaConversions.asJavaIterable(for (actorRef <- childrenActorRefs) yield actorRef.path.toString)).props(),"ProducerMessageRouter")

  override def receive = super.receive orElse {
    case SendMessage => {
      if (!stopped.get) {
        if(!infinite) {
          messagesToSend = messagesToSend - 1

          if (messagesToSend == -1 ) {
            markAsStopped
            notifyMonitor
          }
          else if (messagesToSend < -1) {
            markAsStopped
          }
          else {
            router ! configureQOSHeaders(messageSource.getMessage)
          }
        } else {
          router ! configureQOSHeaders(messageSource.getMessage)
        }

      }
    }
  }



  private def configureQOSHeaders(message : CamelMessage) : CamelMessage = {

    val headers : JMap[String,Any] = new JHashMap(message.getHeaders.size)
    headers.putAll(message.getHeaders)

    if(producer.persistentDelivery) {
      setHeader(JMS_HEADER_DELIVERY_MODE,PERSISTENT,headers)
    } else {
      setHeader(JMS_HEADER_DELIVERY_MODE,NON_PERSISTENT,headers)
    }

    if(producer.timeToLive.gt(Duration.Zero)) {
     setHeader(JMS_HEADER_TTL,System.currentTimeMillis() + producer.timeToLive.toMillis,headers)
    }

    message.withHeaders(headers)
  }

  private def setHeader(headerName : String, value : Any,map : JMap[String,Any]) = {
    if(!map.containsKey(headerName)) {
      map.put(headerName, value)
    }
  }

  private def markAsStopped() : Unit = {
    stopped.set(true)
  }

  private def notifyMonitor() : Unit = {
    self ! ProducerFinished
  }

}
