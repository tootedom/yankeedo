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
package org.greencheek.jms.yankeedo.scenarioexecution

import java.util.concurrent.CountDownLatch
import org.greencheek.jms.yankeedo.structure.scenario.Scenario
import akka.actor.ActorSystem

/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 18:27
 */
trait Executor {
  def execute(scenarioExecutionLatch : CountDownLatch,
              scenario : Scenario, scenarioNumber : Long) : ActorSystem
}
