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
package org.greencheek.jms.yankeedo.scenarioexecution

import akka.actor._
import consumer.ConsumerExecutor
import org.greencheek.jms.yankeedo.structure.scenario.{ScenarioContainer}
import java.util.concurrent.{CountDownLatch}
import org.greencheek.jms.yankeedo.structure.actions.{JmsProducerAction, JmsConsumerAction}
import collection.mutable.ArrayBuffer
import java.util.concurrent.atomic.AtomicLong
import akka.actor.Terminated
import producer.ProducerExecutor
import scala.Some
import concurrent.duration.FiniteDuration
import grizzled.slf4j.Logging

/**
 * User: dominictootell
 * Date: 12/01/2013
 * Time: 13:55
 */
class ScenariosExecutionManager(val applicationLatch : CountDownLatch,
                                val scenarioContainer : ScenarioContainer) extends Actor with Logging {

  val scenariosRunning = new AtomicLong(scenarioContainer.size)



  val scenarioSystems : List[ActorSystem] = {
    val systems = ArrayBuffer[ActorSystem]()

    for ((senario,scenarioNumber) <- scenarioContainer.scenarios.zipWithIndex) {
      systems += ActorSystem("scenariosystem-" + scenarioNumber)
    }

    systems.toList
  }


  val runForDuration : Option[Cancellable] = {
    scenarioContainer.totalDuration match {
      case duration:FiniteDuration => {
        import context.dispatcher
        Some(context.system.scheduler.scheduleOnce(duration) {
          self ! new TerminateExecutingScenariosDurationEnd(duration)
        })
      }
      case _ => {
        None
      }
    }
  }

  def receive = {

    case StartExecutingScenarios => {
      for ( (executionScenario,i) <- scenarioContainer.scenarios.zipWithIndex) {
        val system = scenarioSystems(i)
        val scenarioActor =
          executionScenario.jmsAction match {
            case x:JmsConsumerAction => system.actorOf(Props(new ConsumerExecutor(executionScenario)),"ConsumerExecutor")
            case x:JmsProducerAction => system.actorOf(Props(new ProducerExecutor(executionScenario)),"ProducerExecutor")
          }

        context.watch(scenarioActor)
        info("Starting scenario: " + executionScenario)
        scenarioActor ! ScenarioStart
      }
    }
    case Terminated(x) => {
      recordEndOfScenario
    }
    case x:TerminateExecutingScenariosDurationEnd => {
      info("All Scenarios totalDuration has been reached.  Stopping all scenarios.")
      stopExecution
    }
    case ReturnScenarioActorSystems => {
      sender ! ScenarioActorSystems(scenarioSystems)
    }
  }


  private def recordEndOfScenario() : Unit = {
    val scenariosStillRunning = scenariosRunning.decrementAndGet()
    if (scenariosStillRunning == 0) {
      stopExecution()
    }
  }

  private def stopExecution(): Unit = {


    for (system <- scenarioSystems) {
      try {
        system.shutdown()
        system.awaitTermination()
      } catch {
        case  e: Exception => {
          warn("Unable to stop scenario actor system:" + system.name)
        }
      }
    }

    context.stop(self)

    applicationLatch.countDown()
  }

}
