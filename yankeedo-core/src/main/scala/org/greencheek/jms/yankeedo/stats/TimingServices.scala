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


/**
 * Created by dominictootell on 23/03/2014.
 */
class TimingServices(val useNanoTime : Boolean,
                     val recordStatsImmediately : Boolean) {

  val stats : LatencyInfo = new LatencyInfo()

  var lastMessageTime : Long = recordStatsImmediately match {
    case true => nanoTime()
    case false => -1
  }

  def nanoTime() : Long = {
    if(useNanoTime) {
      System.nanoTime()
    } else {
      System.currentTimeMillis() * 1000000
    }
  }

  def recordStats() = {
    val currTime = nanoTime
    val lastMessageTiming = lastMessageTime
    if(lastMessageTiming != -1) {
      val timeTaken =  currTime - lastMessageTiming
      if (timeTaken >= 0)  {
        stats.record(timeTaken)
      }
    }
    lastMessageTime = currTime
  }

}
