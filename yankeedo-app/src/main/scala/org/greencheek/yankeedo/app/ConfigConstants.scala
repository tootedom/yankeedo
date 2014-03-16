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

/**
 * User: dominictootell
 * Date: 20/01/2013
 * Time: 12:53
 */
object ConfigConstants {
  val YANKEEDO_HOME = Option(System.getProperty("YANKEEDO_HOME",System.getenv("YANKEEDO_HOME"))).getOrElse(".")


  val CONF_DIRECTORY_SCENARIOS = "yankeedo.location.scenarios"
  val CONF_DIRECTORY_BINARIES = "yankeedo.location.binaries"
  val CONF_SCENARIO_NAME_TO_RUN = "yankeedo.scenario.name"

}
