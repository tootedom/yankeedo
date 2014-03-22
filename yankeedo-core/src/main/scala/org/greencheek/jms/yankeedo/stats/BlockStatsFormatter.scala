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

import java.util.concurrent.TimeUnit
import org.LatencyUtils.LatencyStats
import org.HdrHistogram.HistogramData
import scala.concurrent.duration.Duration
import com.dongxiguo.fastring.Fastring.Implicits._

/**
 * Created by dominictootell on 22/03/2014.
 */
object BlockStatsFormatter extends  StatsFormatter {
  val DEFAULT_STATS_OUTPUT_TIMEUNIT = TimeUnit.MILLISECONDS
  val OUTPUT_LENGTH = 80;

  /**
   * Format the LatencyStats to a human readable format.
   *
   * @param scenarioName The name of the scenario the stats are for
   * @param timeunit The timeunit the statistics should be output in
   * @param stats The stats object itself
   * @return
   */
  def formatToString(scenarioName: String, timeunit: TimeUnit, stats: LatencyStats): String = {
    outputStats(scenarioName, stats, timeunit)
  }

  private def calcMessagePerSec(timeUnit: TimeUnit, value: Double): String = {
    if(value < 0) {
      " - "
    } else {
      val doCalc: Boolean = timeUnit match {
        case TimeUnit.SECONDS => true
        case TimeUnit.NANOSECONDS => true
        case TimeUnit.MILLISECONDS => true
        case TimeUnit.MICROSECONDS => true
        case _ => false
      }

      if (doCalc) {
        val requestsPerSec = ((1 / value) * 1000000000)
        f"$requestsPerSec%.2f"
      } else {
        " - "
      }
    }
  }

  private def outputStats(name: String,
                          stats: LatencyStats,
                          timeunit: TimeUnit): String = {

    val abrev = getShortNameForTimeUnit(timeunit)

    val histoData: HistogramData = stats.getAccumulatedHistogram.getHistogramData
    val maxVal: Long = histoData.getMaxValue
    val minVal: Long = histoData.getMinValue
    val meanVal: Double = histoData.getMean
    val stddevVal: Double = histoData.getStdDeviation

    var pp80Val: Double = -1.0
    try {
      pp80Val = histoData.getValueAtPercentile(80.0)
    } catch {
      case e: ArrayIndexOutOfBoundsException => {
        pp80Val = -1.0
      }
    }
    //

    var pp90Val: Double = -1.0
        try {
          pp90Val = histoData.getValueAtPercentile(90.0)
        } catch {
          case e: ArrayIndexOutOfBoundsException => {
            pp90Val = -1.0
          }
        }
    //
    var pp99Val: Double = -1.0
        try {
          pp99Val = histoData.getValueAtPercentile(99.0)
        } catch {
          case e: ArrayIndexOutOfBoundsException => {
            pp99Val = -1.0
          }
        }

    var pp999Val: Double = -1.0
        try {
          pp999Val = histoData.getValueAtPercentile(99.9)
        } catch {
          case e: ArrayIndexOutOfBoundsException => {
            pp999Val = -1.0
          }
        }


    val p80Val: Double = pp80Val
    val p90Val: Double = pp90Val
    val p99Val: Double = pp99Val
    val p999Val: Double = pp999Val
    val total: Double = histoData.getTotalCount


    val s = fast"""
${rightPad("================================================================================", 80)}
${rightPad(name, 80)}
${rightPad("================================================================================", 80)}
${formatLine("number of messages: ", total)}
${formatLine("min value:", toTimeUnit(minVal, timeunit), abrev)}
${formatLine("max value:", toTimeUnit(maxVal, timeunit), abrev)}
${formatLine("mean:", toTimeUnit(meanVal, timeunit), abrev + " (" + calcMessagePerSec(timeunit, meanVal) + " msg/sec)")}
${formatLine("stddev:", toTimeUnit(stddevVal, timeunit), abrev + " (" + calcMessagePerSec(timeunit, stddevVal) + " msg/sec)")}
${formatLine("80%ile:", toTimeUnit(p80Val, timeunit), abrev + " (" + calcMessagePerSec(timeunit, p80Val) + " msg/sec)")}
${formatLine("90%ile:", toTimeUnit(p90Val, timeunit), abrev + " (" + calcMessagePerSec(timeunit, p90Val) + " msg/sec)")}
${formatLine("99%ile:", toTimeUnit(p99Val, timeunit), abrev + " (" + calcMessagePerSec(timeunit, p99Val) + " msg/sec)")}
${formatLine("99.9%ile:", toTimeUnit(p999Val, timeunit), abrev + " (" + calcMessagePerSec(timeunit, p999Val) + " msg/sec)")}
${rightPad("================================================================================", 80)}
""".toString


    s.toString
  }

  private def toTimeUnit(value: Double, timeUnit: TimeUnit): Double = {
    if(value<0) {
      -1
    } else {
      val duration = Duration(value, TimeUnit.NANOSECONDS)
      duration.toUnit(timeUnit)
    }
  }

  private def toTimeUnit(value: Long, timeUnit: TimeUnit): Double = {
    if(value<0) {
      -1
    } else {
      val duration = Duration(value, TimeUnit.NANOSECONDS)
      duration.toUnit(timeUnit)
    }
  }


  private def formatLine(name: String, value: Double, trail: String = ""): Fastring = {
    if(value < 0) {
      fast"${rightPad(name, OUTPUT_LENGTH - 32)} ${leftPad(" - ", 7)}${trail}"
    } else {
      fast"${rightPad(name, OUTPUT_LENGTH - 32)} ${leftPad(printable(value), 7)}${trail}"
    }

  }

  private def leftPad(string: String, length: Int, padder: String = " ") = {
    val paddingLength = length - string.length
    if (paddingLength > 0)
      padder * paddingLength + string
    else
      string
  }

  private def rightPad(string: String, length: Int, padder: String = " ") = {
    val paddingLength = length - string.length
    if (paddingLength > 0)
      string + padder * paddingLength
    else
      string
  }

  private def printable(value: Double) = f"$value%.2f"

  private def getShortNameForTimeUnit(timeUnit: TimeUnit): String = {
    timeUnit match {
      case TimeUnit.SECONDS => " s"
      case TimeUnit.DAYS => " d"
      case TimeUnit.HOURS => " h"
      case TimeUnit.MILLISECONDS => " ms"
      case TimeUnit.NANOSECONDS => " ns"
      case TimeUnit.MINUTES => " m"
      case TimeUnit.MICROSECONDS => " µ"
    }
  }

}
