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

import org.greencheek.jms.yankeedo.scenarioexecution.{ScenarioStart, ExecutionMonitorFinished}
import org.greencheek.jms.yankeedo.structure.scenario.Scenario
import akka.actor._
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import org.greencheek.jms.yankeedo.structure.actions.{JmsProducerAction => Producer}
import akka.camel.CamelMessage
import akka.actor.Status.Failure
import grizzled.slf4j.Logging
import scala.concurrent.duration._
import org.greencheek.jms.yankeedo.stats.TimingServices


/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 18:26
 */
class ProducerExecutor(val scenario : Scenario,
                       val statsRecorder : TimingServices) extends Actor with Logging {

  var messagesSendOk = 0
  var messagesNotSendOk = 0
  val producer = context.actorOf(Props(new ProducerMessageRouter(scenario,producerActorCreation(scenario,self,_,_))),"producermonitor")
  val isScheduledMessageSender = scenario.jmsAction.asInstanceOf[Producer].delayBetweenMessages.isInstanceOf[FiniteDuration]

  val started = new AtomicBoolean(false)
  val productMonitorTerminated = new AtomicBoolean(false)

  context.watch(producer)

  val runForDuration : Option[Cancellable] = {
    scenario.jmsAction.asInstanceOf[Producer].delayBetweenMessages match {
      case duration:FiniteDuration => {
        import context.dispatcher
        Some(context.system.scheduler.schedule(duration,duration) {
          if (started.get() && !productMonitorTerminated.get())  {
            producer ! SendMessage
          }
        })
      }
      case _ => {
        None
      }
    }
  }

  val isExecutedByScheduler : Boolean = runForDuration match {
    case Some(_) => true
    case None => false
  }

  override def receive = {
    case ExecutionMonitorFinished => {
      started.set(false)
      debug("Producer Executor received ExecutionMonitorFinished for scenario:" + scenario)
      stopSendScheduler
      context.stop(self)
    }
    case ScenarioStart => {
      started.set(true)
      if(!isExecutedByScheduler) {
        producer ! SendMessage
      }
    }
    case camel : CamelMessage => {
      messagesSendOk+=1
      sendMessage
      recordStats()
    }
    case failure : Failure => {
      messagesNotSendOk += 1
      sendMessage
      recordStats()
    }
    case Terminated(`producer`) => {
      productMonitorTerminated.set(true)
    }
  }

  private def recordStats() = {
    if(started.get) {
      statsRecorder.recordStats()
    }
  }

  override def postStop = {
    stopSendScheduler()
  }

  private def stopSendScheduler() = {
    runForDuration match {
      case Some(x) => {
        if(!x.isCancelled) x.cancel()
      }
      case None => {}
    }
  }

  private def sendMessage = {
    if (!isScheduledMessageSender) {
      producer ! SendMessage
    }
  }

  private def producerActorCreation(scenario : Scenario, responseReciever : ActorRef, ctx : ActorContext, numberOfMessages : AtomicLong) : List[ActorRef] =  {
    var actorRefs : List[ActorRef] = Nil
    for( i <- 1 to scenario.numberOfActors) {
      actorRefs ::= createActor(i,scenario,responseReciever,ctx)
    }
    actorRefs
  }

  private def createActor(actorNumber : Int, scenario : Scenario,responseReciever : ActorRef,ctx : ActorContext) : ActorRef = {
    ctx.actorOf(Props(new AkkaProducer(scenario.jmsAction.asInstanceOf[Producer],responseReciever)),"ProducerActor"+actorNumber)
  }
}
