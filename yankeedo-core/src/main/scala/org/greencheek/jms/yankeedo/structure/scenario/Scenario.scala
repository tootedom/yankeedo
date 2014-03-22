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
import concurrent.duration.{FiniteDuration, Duration}
import org.LatencyUtils.LatencyStats.Builder
import org.LatencyUtils.{PauseDetector, LatencyStats, SimplePauseDetector}
import java.util.concurrent.TimeUnit
import java.io.OutputStream
import org.HdrHistogram.{HistogramIterationValue, HistogramData}
import com.dongxiguo.fastring.Fastring.Implicits._


object Scenario {
  val PAUSE_DETECTOR : PauseDetector = new SimplePauseDetector()
  val DEFAULT_STATS_OUTPUT_TIMEUNIT = TimeUnit.MILLISECONDS
  val OUTPUT_LENGTH = 80;
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
  def outputStats() : Unit = {
    outputStats(System.out,Scenario.DEFAULT_STATS_OUTPUT_TIMEUNIT)
  }

  def outputStats(timeUnit : TimeUnit) : Unit = {
    outputStats(System.out,timeUnit)
  }

  def calcMessagePerSec(timeUnit : TimeUnit, value : Double) : String = {
    val doCalc : Boolean = timeUnit match {
      case TimeUnit.SECONDS => true
      case TimeUnit.NANOSECONDS => true
      case TimeUnit.MILLISECONDS => true
      case TimeUnit.MICROSECONDS => true
      case _ => false
    }

    if(doCalc) {
      val requestsPerSec = ((1/value) * 1000000000)
      f"$requestsPerSec%.2f"
    } else {
      " - "
    }
  }

  def outputStats(out : OutputStream,
                  timeunit : TimeUnit) : Unit = {

    val abrev = getShortNameForTimeUnit(timeunit)

    val histoData : HistogramData = stats.getAccumulatedHistogram.getHistogramData
    val maxVal : Long = histoData.getMaxValue
    val minVal : Long = histoData.getMinValue
    val meanVal : Double = histoData.getMean
    val stddevVal : Double = histoData.getStdDeviation
    val p90Val : Double = histoData.getValueAtPercentile(90.0)
    val p99Val : Double = histoData.getValueAtPercentile(99.0)
    val p999Val : Double = histoData.getValueAtPercentile(99.9)
    val total : Double = histoData.getTotalCount


    val s = fast"""
${rightPad("================================================================================",80)}
${rightPad(name,80)}
${rightPad("================================================================================",80)}
${formatLine("number of requests: ",total)}
${formatLine("min value:",toTimeUnit(minVal,timeunit),abrev)}
${formatLine("max value:",toTimeUnit(maxVal,timeunit),abrev)}
${formatLine("mean:",toTimeUnit(meanVal,timeunit),abrev+" (" + calcMessagePerSec(timeunit,meanVal) + " msg/sec)")}
${formatLine("stddev:",toTimeUnit(stddevVal,timeunit),abrev+" (" + calcMessagePerSec(timeunit,stddevVal) + " msg/sec)")}
${formatLine("90%ile:",toTimeUnit(p90Val,timeunit),abrev+" (" + calcMessagePerSec(timeunit,p90Val) + " msg/sec)")}
${formatLine("99%ile:",toTimeUnit(p99Val,timeunit),abrev+" (" + calcMessagePerSec(timeunit,p99Val) + " msg/sec)")}
${formatLine("99.9%ile:",toTimeUnit(p999Val,timeunit),abrev+" (" + calcMessagePerSec(timeunit,p999Val) + " msg/sec)")}
${rightPad("================================================================================",80)}
""".toString


    out.write(s.toString().getBytes)
  }

  private def toTimeUnit(value: Double, timeUnit : TimeUnit) : Double =
  {
    val duration = Duration(value,TimeUnit.NANOSECONDS)
    duration.toUnit(timeUnit)
  }

  private def toTimeUnit(value: Long, timeUnit : TimeUnit) : Double =
  {
    val duration = Duration(value,TimeUnit.NANOSECONDS)
    duration.toUnit(timeUnit)
  }



  private def formatLine(name : String, value : Double, trail : String ="") : Fastring = {
    fast"${rightPad(name,Scenario.OUTPUT_LENGTH - 32)} ${leftPad(printable(value),7)}${trail}"

  }

  def leftPad(string: String, length: Int, padder: String = " ") = {
    val paddingLength = length - string.length
    if (paddingLength > 0)
      padder * paddingLength + string
    else
      string
  }

  def rightPad(string : String,length: Int, padder: String = " ") = {
    val paddingLength = length - string.length
    if (paddingLength > 0)
      string + padder * paddingLength
    else
      string
  }

  def printable(value: Double) = f"$value%.2f"

  private def getShortNameForTimeUnit(timeUnit : TimeUnit) : String = {
    timeUnit match {
      case TimeUnit.SECONDS => "s"
      case TimeUnit.DAYS => "d"
      case TimeUnit.HOURS => "h"
      case TimeUnit.MILLISECONDS => "ms"
      case TimeUnit.NANOSECONDS => "ns"
      case TimeUnit.MINUTES => "m"
      case TimeUnit.MICROSECONDS => "µ"
    }
  }

}

