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
package org.greencheek.jms.yankeedo.app

import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer
import java.util.concurrent.{TimeUnit, CountDownLatch}
import akka.actor.{Props, ActorSystem}
import scala.concurrent.duration._
import org.greencheek.jms.yankeedo.scenarioexecution.{TerminateExecutingScenariosDurationEnd, StartExecutingScenarios, ScenariosExecutionManager}
import grizzled.slf4j.Logging
import akka.pattern.ask

/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 15:41
 *
 * Executes the given scenarios
 */
object ScenarioContainerExecutor extends Logging {

  val ACTOR_SYSTEM_NAME : String = "yankeedoo-scenario-system"

  def executeScenarios(scenariosToRun: ScenarioContainer) : Int = {
    executeScenarios(scenariosToRun,Duration.Inf)
  }

  def executeScenarios(scenariosToRun: ScenarioContainer, until : Duration) : Int = {

    val appLatch = new CountDownLatch(1)
    val actorSystem = ActorSystem(ACTOR_SYSTEM_NAME)

    val scenarioExecutor = actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,scenariosToRun)))

    scenarioExecutor ! StartExecutingScenarios

    var hasTimeout = false
    until match {
      case Duration.Inf => hasTimeout = false
      case Duration.MinusInf => hasTimeout = false
      case _ => hasTimeout = true
    }

    var executedWithoutTimeout = false
    if(hasTimeout) {
      try {
        executedWithoutTimeout = appLatch.await(until.toMillis,TimeUnit.MILLISECONDS)
        if(!executedWithoutTimeout) {
          scenarioExecutor ! new TerminateExecutingScenariosDurationEnd(until)
        }
      } catch {
        case e: Exception => {
          executedWithoutTimeout = false
          error("Exception whilst waiting for scenario to complete",e)
        }
      }
    } else {
      try {
        appLatch.await()
        executedWithoutTimeout = true
      } catch {
        case e: Exception => {
          executedWithoutTimeout = false

          error("Exception whilst waiting for scenario to complete",e)
        }
      }
    }

    try {
      actorSystem.shutdown()
      actorSystem.awaitTermination()
    } catch {
      case e : Exception => {
        error("Exception whilst shutting down scenario executor actor system",e)
      }
    } finally {
      if (scenariosToRun.outputStats) {
        for(scenario <- scenariosToRun.scenarios) {
          scenario.outputStats()
        }
      }
    }

    if(executedWithoutTimeout) {
      1
    } else {
      0
    }

  }

}
