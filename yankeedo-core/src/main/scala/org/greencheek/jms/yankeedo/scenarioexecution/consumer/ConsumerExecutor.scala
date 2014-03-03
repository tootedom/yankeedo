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

import org.greencheek.jms.yankeedo.structure.scenario.Scenario
import akka.actor._
import org.greencheek.jms.yankeedo.structure.actions.{JmsConsumerAction => Consumer}
import org.greencheek.jms.yankeedo.scenarioexecution._
import java.util.concurrent.atomic.AtomicLong
import grizzled.slf4j.Logging


/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 15:56
 */
class ConsumerExecutor(val scenario : Scenario) extends Actor with Logging {


  def receive = {
    case ExecutionMonitorFinished => {
      debug("Consumer Executor received ExecutionMonitorFinished for scenario:" + scenario)
      context.stop(self)
    }
    case ScenarioStart => {
      context.actorOf(Props(
        new ScenarioExecutionMonitor(scenario,consumerActorCreation(scenario,_,_))))
    }
    case _ => {
    }
  }

  private def consumerActorCreation(scenario : Scenario, ctx : ActorContext, messagesToProcess : AtomicLong) : List[ActorRef] =  {
    var actorRefs : List[ActorRef] = Nil
    for( i <- 1 to scenario.numberOfActors) {
      actorRefs ::= createActor(scenario,ctx,messagesToProcess)
    }
    actorRefs
  }

  private def createActor(scenario: Scenario, ctx: ActorContext, messagesToProcess: AtomicLong): ActorRef = {
    ctx.actorOf(Props(
      new AkkaConsumer(scenario.jmsAction.asInstanceOf[Consumer],
                       scenario.jmsAction.asInstanceOf[Consumer].messageProcessor,
                       messagesToProcess)))
  }
}
