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
package org.greencheek.yankeedo.app

import grizzled.slf4j.Logging


import CommandLineConstants._
import com.typesafe.config.ConfigFactory
import java.util.{HashMap => JMap}
import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer

import scopt.mutable.OptionParser
import org.greencheek.app.compilation.scenario.ScenarioClassLoader
import java.util.concurrent.CountDownLatch
import akka.actor.{Props, ActorSystem}
import org.greencheek.jms.yankeedo.scenarioexecution.{StartExecutingScenarios, ScenariosExecutionManager}
import java.io.{File => JFile}
import java.net.URLClassLoader

/**
 * User: dominictootell
 * Date: 19/01/2013
 * Time: 15:03
 */
object Yankeedo extends App with Logging {

  val INCORRECT_ARGUMENTS = -1;
  val FINISHED = 0;
  val SCENARIO_TIMEOUT = 1;
  val NO_SCENARIOS_FOUND = 2;


  sys.exit(runYankeedo(args))

  def runYankeedo(args: Array[String]) : Int = {
    val props = YankeedoCommandLineArgs

    val cliOptsParser = new OptionParser("gatling") {
      opt(CLI_SCENARAIOS_FOLDER, CLI_SCENARAIOS_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> to discover scenarios that could be run", { v: String => props.sourcesDirectory(v) })
      opt(CLI_SCENARAIOS_BINARIES_FOLDER, CLI_SCENARAIOS_BINARIES_FOLDER_ALIAS, "<directoryPath>", "Uses <directoryPath> to discover already compiled simulations", { v: String => props.binariesDirectory(v) })
      opt(CLI_SCENARAIO_NAME, CLI_SCENARAIO_NAME_ALIAS, "<className>", "Runs <className> scenario", { v: String => props.runScenarioWithName(v) })
      help(CLI_HELP,CLI_HELP_ALIAS,"displays the options available")
    }

    // if arguments are incorrect, usage message is displayed
    if (cliOptsParser.parse(args)) runWithConfig(props.build)
    else INCORRECT_ARGUMENTS
  }

  def runWithConfig(props : JMap[String,Any]) : Int = {

    val classLoader = getClass.getClassLoader

    val defaultsConfig = ConfigFactory.parseResources(classLoader, "yankeedo-defaults.conf")
    val customConfig = ConfigFactory.parseResources(classLoader, "yankeedo.conf")
    val propertiesConfig = ConfigFactory.parseMap(props,null)


    val config = propertiesConfig.withFallback(customConfig).withFallback(defaultsConfig)

    val runtimeConfig = new YankeedoRuntimeConfiguration(config)


    val scenarios = runtimeConfig.binariesDirectory
      .map(ScenarioClassLoader.fromClasspathBinariesDirectory) // expect simulations to have been pre-compiled (ex: IDE)
      .getOrElse(ScenarioClassLoader.fromSourcesDirectory(runtimeConfig.sourcesDirectory))
      .simulationClasses(runtimeConfig.scenarioNameToRun)
      .sortWith(_.getName < _.getName)


    scenarios.size match {
      case 0 =>  {
        info("No scenarios are available to run.  Classpath is: " )
        val classpath =  Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader].getURLs.map(url => new JFile(url.toURI))
        for ((item,index) <- classpath.zipWithIndex) {
          info("ClassPath Item("+index+"):" + item)
        }
        NO_SCENARIOS_FOUND
      }
      case _ => {
        val scenario = runtimeConfig.scenarioNameToRun match {
          case Some(x : String) => {
            val matchingRequestedScenario = scenarios.filter {
              _.toString.matches(x)
            }

            if(matchingRequestedScenario.size == 0) {
              info("Unable to locate specified scenario: " + runtimeConfig.scenarioNameToRun)
              info("However, other scenarios are available.  Please choose one:")
              interactiveSelect(scenarios,runtimeConfig)
            } else {
              matchingRequestedScenario(0).newInstance()
            }
          }
          case None => {
            interactiveSelect(scenarios,runtimeConfig)
          }
        }

        runScenario(scenario)
      }
    }
  }

  private def runScenario(scenario : ScenarioContainer) : Int = {


    val appLatch = new CountDownLatch(1)
    val actorSystem = ActorSystem()

    val scenarioExecutor = actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,scenario)))

    scenarioExecutor ! StartExecutingScenarios

    appLatch.await()

    actorSystem.shutdown()

    FINISHED
  }

  private def defaultOutputDirectoryBaseName(clazz: Class[ScenarioContainer]) = clazz.getSimpleName

  private def interactiveSelect(simulations: List[Class[ScenarioContainer]], runtimeConfig : YankeedoRuntimeConfiguration): ScenarioContainer = {

    val simulation = selectSimulationClass(simulations,runtimeConfig)

    val myDefaultOutputDirectoryBaseName = defaultOutputDirectoryBaseName(simulation)

    simulation.newInstance
  }

  private def selectSimulationClass(simulations: List[Class[ScenarioContainer]],
                                    runtimeConfig : YankeedoRuntimeConfiguration): Class[ScenarioContainer] = {

    val selection = simulations.size match {
      case 0 =>
        // If there is no simulation file
        info("There is no scenario script. Please check that your scripts are in " + runtimeConfig.sourcesDirectory)
        sys.exit
      case 1 =>
        info(simulations.head.getName + " is the only available scenario, executing it.")
        0
      case size =>
        println("Choose a scenario number to run:")
        for ((simulation, index) <- simulations.zipWithIndex) {
          println("     [" + index + "] " + simulation.getName)
        }
        Console.readInt
    }

    val validRange = 0 until simulations.size
    if (validRange contains selection)
      simulations(selection)
    else {
      println("Invalid selection, must be in " + validRange)
      selectSimulationClass(simulations,runtimeConfig)
    }
  }




}
