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

import org.greencheek.jms.yankeedo.scenarioexecution.{ProducerFinished, ScenarioExecutionMonitor}
import org.greencheek.jms.yankeedo.structure.scenario.Scenario
import akka.actor.{Props, ActorRef, ActorContext}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import akka.routing.{RoundRobinRouter, RoundRobinGroup}
import collection.JavaConversions
import org.greencheek.jms.yankeedo.structure.actions.{JmsProducerAction => Producer, Queue, Topic}

/**
 * User: dominictootell
 * Date: 12/01/2013
 * Time: 19:39
 */
class ProducerMessageRouter(scenario : Scenario,
                            children : (ActorContext, AtomicLong) => List[ActorRef]) extends ScenarioExecutionMonitor(scenario,children) {


  val messagesToSend = new AtomicLong(scenario.numberOfMessages)
  val infinite : Boolean = if(messagesToSend.get() == -1) true else false
  val stopped = new AtomicBoolean(false)


  val router = context.actorOf(new RoundRobinGroup(JavaConversions.asJavaIterable(for (actorRef <- childrenActorRefs) yield actorRef.path.toString)).props(),"ProducerMessageRouter")

  override def receive = super.receive orElse {
    case SendMessage => {
      if (!stopped.get) {
        if(!infinite) {
          val currentMessageNumber = messagesToSend.decrementAndGet()

          if (currentMessageNumber == -1 ) {
            markAsStopped
            notifyMonitor
          }
          else if (currentMessageNumber < -1) {
            markAsStopped
          }
          else {
            router ! scenario.jmsAction.asInstanceOf[Producer].messageSource.getMessage
          }
        } else {
          router ! scenario.jmsAction.asInstanceOf[Producer].messageSource.getMessage
        }

      }
    }
  }

  private def markAsStopped() : Unit = {
    stopped.set(true)
  }

  private def notifyMonitor() : Unit = {
    self ! ProducerFinished
  }

}
