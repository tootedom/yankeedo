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
package org.greencheek.jms.yankeedo.stats

import org.LatencyUtils.{SimplePauseDetector, PauseDetector, LatencyStats}
import org.LatencyUtils.LatencyStats.Builder


object LatencyInfo {
  val PAUSE_DETECTOR: PauseDetector = new SimplePauseDetector()
}
/**
 * Created by dominictootell on 13/07/2014.
 */
class LatencyInfo {
  val stats : LatencyStats = Builder.create().pauseDetector(LatencyInfo.PAUSE_DETECTOR).build()
  var numberOfStatsRequestedRecording : Long= 0l

  def record(timeTaken : Long) : Unit = {
    stats.recordLatency(timeTaken)
    numberOfStatsRequestedRecording+=1
  }

  def getStats : LatencyStats = {
    stats
  }

  def getNumberOfStatsRequestedRecording : Long = {
    numberOfStatsRequestedRecording
  }

}
