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

import concurrent.duration.Duration
import org.greencheek.jms.yankeedo.structure.scenario.Scenario

/**
 * User: dominictootell
 * Date: 07/01/2013
 * Time: 22:46
 */
abstract class ScenarioActorMessage(val message : String = "")
case object ConsumerFinished extends ScenarioActorMessage
case object ProducerFinished extends ScenarioActorMessage
case object ExecutionMonitorFinished extends ScenarioActorMessage
case object ScenarioStart extends ScenarioActorMessage
case class ScenarioExecutionMonitorRunDurationFinished(val durationThatExpired : Duration) extends ScenarioActorMessage("Run For Duration Expired: " + durationThatExpired)
case class ScenarioExecutionFinished(val scenario : Scenario) extends ScenarioActorMessage
case object StartExecutingScenarios extends ScenarioActorMessage
case class TerminateExecutingScenariosDurationEnd(val durationThatExpired : Duration) extends ScenarioActorMessage("All Scenarios run for duration, that has now expired: " + durationThatExpired)
