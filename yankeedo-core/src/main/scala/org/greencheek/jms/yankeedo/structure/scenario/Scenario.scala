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

import org.greencheek.jms.yankeedo.structure.actions.JmsAction
import org.greencheek.jms.yankeedo.config.JmsConfiguration
import concurrent.duration.Duration
import org.LatencyUtils.LatencyStats.Builder
import org.LatencyUtils.{PauseDetector, LatencyStats, SimplePauseDetector}
import org.greencheek.jms.yankeedo.stats.OutputStats


object Scenario {
  val PAUSE_DETECTOR : PauseDetector = new SimplePauseDetector()
}

/**
 * User: dominictootell
 * Date: 02/01/2013
 * Time: 08:36
 */
class Scenario(val runForDuration: Duration = Duration.Inf,
               val numberOfMessages : Long = -1,
               val numberOfActors : Int = 1,
               val jmsUrl : String,
               val jmsAction : JmsAction,
               val jmsConfiguration : Option[JmsConfiguration],
               val recordStatsImmediately : Boolean,
               val name : String
          )
{
  val stats : LatencyStats = Builder.create().pauseDetector(Scenario.PAUSE_DETECTOR).build()


  override def toString = {
    val buf = new StringBuilder

    buf ++= "scenario"
    name.length match {
      case 0 => {}
      case _ => buf += '(' ++= name += ')'
    }
    buf ++= ":("
    buf ++= "jms-action-type=" ++= jmsAction.toString += ','
    buf ++= "run-for-duration=" ++= runForDuration.toString +=  ','
    buf ++= "number-of-messages=" ++= numberOfMessages.toString += ','
    buf ++= "number-of-actors=" ++= numberOfActors.toString += ','
    buf ++= "jms-url=" ++= jmsUrl
    buf += ')'

    buf.toString()
  }

  // ADD OUTPUTTING THE STATS
  def outputStats(statsConfig : OutputStats) : Unit = {
    val statsFormat : String = statsConfig.formatter.formatToString(name,statsConfig.timeUnit,stats)
    try {
      statsConfig.writer.write(statsFormat)
      statsConfig.writer.flush()
    } catch {
      case e: Exception => {

      }
    }
  }



}

