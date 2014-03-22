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
package org.greencheek.jms.yankeedo.structure.scenario

import concurrent.duration.Duration
import org.greencheek.jms.yankeedo.stats.{DefaultOutputStats, OutputStats}

/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 15:39
 *
 * Interface for uses to extend to provide their custom scenarios
 */
trait ScenarioContainer {

  @volatile private var _outputStatsEnabled : Boolean = false;
  @volatile private var _outputStatsOptions : Option[OutputStats] = None;
  @volatile private var _totalDuration : Duration = Duration.Inf
  @volatile private var _scenarios : Seq[Scenario] = Nil

  /**
   * The number of defined scenarios
   * @return
   */
  final def size : Int = this.scenarios.size



  final def outputStatsOptions(outputStatsOption : Option[OutputStats] ) : ScenarioContainer = {
    _outputStatsOptions = outputStatsOption
    _outputStatsEnabled = outputStatsOption match {
      case Some(_) =>  true
      case None => false
    }
    this
  }

  /**
   * output stats to the defaults (System.out)
   * @return
   */
  final def outputStatus() : ScenarioContainer = {
    _outputStatsOptions = Some(new DefaultOutputStats)
    _outputStatsEnabled = true
    this
  }

  /**
   * The amount of time given for all scenarios to execute in, other wise the
   * app will terminate the scenarios, and shut down.
   * @param totalDuration
   */
  final def runFor(totalDuration : Duration) : ScenarioContainer = {
    _totalDuration = totalDuration
    this
  }

  /**
   * Passes the list of scenarios to be run
   * @param scenariosToRun
   */
  final def withScenarios(scenariosToRun : Seq[Scenario]) : ScenarioContainer = {
    _scenarios = scenariosToRun
    this
  }

  /**
   * Adds a scenario to the start of all existing scenarios to be run.  The scenarios are started
   * in the order they appear in the list
   * @param scenario
   * @return
   */
  final def addScenario(scenario : Scenario) : ScenarioContainer = {
    scenario +: _scenarios
    this
  }

  /**
   * Adds a scenario to the end of the list of existing scenarios to be run.  The scenarios are
   * started in the order they appear in the list.  This operation is slower than that of addScenario
   * @see http://www.scala-lang.org/docu/files/collections-api/collections_40.html
   *
   * @param scenario Scenario to add to the end of the list of scenarios
   */
  final def appendScenario(scenario : Scenario) : ScenarioContainer = {
    _scenarios = _scenarios :+ scenario
    this
  }


  final def totalDuration = {
    _totalDuration
  }

  final def scenarios  = {
    _scenarios
  }

  final def outputStatsEnabled : Boolean = {
    _outputStatsEnabled
  }

  final def outputStatsOptions : Option[OutputStats] = {
    _outputStatsOptions
  }

}

object ScenarioContainer {
  def apply(scenarios : Scenario*) = {
    val scenarioContainer = new Object with ScenarioContainer
    scenarioContainer.withScenarios(scenarios)
  }
}
