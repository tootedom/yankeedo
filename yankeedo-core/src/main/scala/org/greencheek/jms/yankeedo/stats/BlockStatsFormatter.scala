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
import org.HdrHistogram.{AbstractHistogram, Histogram, HistogramData, HistogramIterationValue}
import scala.concurrent.duration.Duration
import com.dongxiguo.fastring.Fastring.Implicits._
import scala.collection.JavaConversions._
import scala.collection.SortedSet

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
  def formatToString(scenarioName: String, timeunit: TimeUnit, stats: LatencyInfo): String = {
    outputStats(scenarioName, stats, timeunit)
  }

  private def calcMessagePerSec(timeUnit: TimeUnit, value: Double): String = {
    if(value <= 0) {
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
        val requestsPerSec = ((1.0 / value) * 1000000000.0)
        f"$requestsPerSec%.2f"
      } else {
        " - "
      }
    }
  }

  private def getPercentileValue(histoData : Histogram, percentileToObtain : Double) : Long = {
    try {
      histoData.getValueAtPercentile(percentileToObtain)
    }  catch {
      case e: ArrayIndexOutOfBoundsException => {
        return 0
      }
    }
  }

  private def getPercentiles(histoData : Histogram) : SortedSet[Double] = {
    val p : AbstractHistogram#Percentiles = histoData.percentiles(1)

    val defaultSortedSet = scala.collection.mutable.SortedSet[Double]()
    for(value : HistogramIterationValue <-  p.iterator()) {
      val percentageValue = value.getPercentile
      if(percentageValue>=60) {
        defaultSortedSet += percentageValue
      }
    }

    val availablePercentilesSortedSet = scala.collection.mutable.SortedSet[Double]()

    var pp80Val: Double = getPercentileValue(histoData,80.0)
    if(pp80Val!=0) availablePercentilesSortedSet.add(80.0)


    var pp90Val: Double = getPercentileValue(histoData,90.0)
    if(pp90Val!=0) availablePercentilesSortedSet.add(90.0)

    var pp99Val: Double = getPercentileValue(histoData,99.0)
    if(pp99Val!=0) availablePercentilesSortedSet.add(99.0)

    var pp999Val: Double = getPercentileValue(histoData,99.9)
    if(pp999Val!=0) availablePercentilesSortedSet.add(99.9)

    if(availablePercentilesSortedSet.size <3) {
      defaultSortedSet
    } else {
      SortedSet(80.0,90.0,99.0,99.9)
    }
  }

  private def outputStats(name: String,
                          latencyInfo: LatencyInfo,
                          timeunit: TimeUnit): String = {

    val stats : LatencyStats = latencyInfo.getStats
    val abrev = getShortNameForTimeUnit(timeunit)
    stats.forceIntervalSample()
    val histoData: Histogram = stats.getAccumulatedHistogram

    val percentiles : SortedSet[Double] = getPercentiles(histoData)

    val maxVal: Long = histoData.getMaxValue
    val minVal: Long = histoData.getMinValue
    val meanVal: Double = histoData.getMean
    val stddevVal: Double = histoData.getStdDeviation


    val total: Double = histoData.getTotalCount

    val percentilesStringBuilder = new StringBuilder(1000)
    percentilesStringBuilder.append(fast"""
${rightPad("================================================================================", 80)}
${rightPad(name, 80)}
${rightPad("================================================================================", 80)}
${formatLine("number of messages: ", latencyInfo.getNumberOfStatsRequestedRecording)}
${formatLine("number of recorded stats: ", total)}
${formatLine("min value:", toTimeUnit(minVal, timeunit), abrev)}
${formatLine("max value:", toTimeUnit(maxVal, timeunit), abrev)}
${formatLine("mean:", toTimeUnit(meanVal, timeunit), abrev + " (" + calcMessagePerSec(timeunit, meanVal) + " msg/sec)")}
${formatLine("stddev:", toTimeUnit(stddevVal, timeunit), abrev + " (" + calcMessagePerSec(timeunit, stddevVal) + " msg/sec)")}\n""".toString)

    percentilesStringBuilder.append(createPercentilesString(percentiles,timeunit,abrev,histoData))

//${formatLine("80%ile:", toTimeUnit(p80Val, timeunit), abrev + " (" + calcMessagePerSec(timeunit, p80Val) + " msg/sec)")}
//${formatLine("90%ile:", toTimeUnit(p90Val, timeunit), abrev + " (" + calcMessagePerSec(timeunit, p90Val) + " msg/sec)")}
//${formatLine("99%ile:", toTimeUnit(p99Val, timeunit), abrev + " (" + calcMessagePerSec(timeunit, p99Val) + " msg/sec)")}
//${formatLine("99.9%ile:", toTimeUnit(p999Val, timeunit), abrev + " (" + calcMessagePerSec(timeunit, p999Val) + " msg/sec)")}
    percentilesStringBuilder.append(fast"""\n${rightPad("================================================================================", 80)}\n""".toString)
//""".toString


    percentilesStringBuilder.toString
  }

  private def createPercentilesString(percentiles : SortedSet[Double],timeunit : TimeUnit,
                                      abrev : String, histdata: Histogram) : String = {
    val builder : StringBuilder = new StringBuilder(500)
    for(percentile : Double <- percentiles) {
      val pString = f"$percentile%.2f"+"%ile:"
      val pValue = histdata.getValueAtPercentile(percentile)
      builder.append(
      fast"""${formatLine(pString, toTimeUnit(pValue, timeunit), abrev + " (" + calcMessagePerSec(timeunit, pValue) + " msg/sec)")}\n""".toString())
    }

    builder.toString
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
