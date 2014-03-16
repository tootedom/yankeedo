/**
 * Copyright 2012-2014 greencheek.org (www.greencheek.org)
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

import message.CamelMessageSource
import org.greencheek.jms.yankeedo.structure.actions.{JmsProducerAction => JmsProd, Queue, Topic}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import akka.camel.{CamelMessage, Oneway, Producer}
import org.greencheek.jms.yankeedo.scenarioexecution.ProducerFinished
import akka.actor.{Terminated, ActorRef, Actor, ActorInitializationException}
import akka.actor.Status.Failure

/**
 * User: dominictootell
 * Date: 08/01/2013
 * Time: 08:23
 */
class AkkaProducer(val jmsAction : JmsProd, responseReciever : ActorRef) extends  Producer with Oneway{

  val endpoint  = {
    jmsAction destination match {
      case Topic(destName) => "jms:topic:" + destName
      case Queue(destName) => "jms:queue:" + destName
    }
  }

  override def endpointUri = endpoint

  override def routeResponse(msg: Any) = {
    msg match {
      case message : CamelMessage => responseReciever ! msg
      case failure : Failure => {
        responseReciever ! failure
      }
    }
  }



}
