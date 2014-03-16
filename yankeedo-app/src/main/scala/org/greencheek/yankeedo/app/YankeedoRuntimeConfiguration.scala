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

import com.typesafe.config.Config
import ConfigConstants._

import scala.tools.nsc.io.{ Path, Directory }
import scala.tools.nsc.io.Path.string2path
/**
 * User: dominictootell
 * Date: 19/01/2013
 * Time: 17:27
 */

class YankeedoRuntimeConfiguration(private val config : Config) {


  def sourcesDirectory: Directory = resolvePath(trimToOption(config.getString(CONF_DIRECTORY_SCENARIOS))).getOrElse(
    YANKEEDO_HOME / "user-files/scenarios").toDirectory

  def binariesDirectory: Option[Directory] = resolveDir(resolvePath(trimToOption(config.getString(CONF_DIRECTORY_BINARIES))))

  def scenarioNameToRun : Option[String]  = trimToOption(config.getString(CONF_SCENARIO_NAME_TO_RUN))


  /*
   * Internal methods below.
   *
   */

  private def trimToOption(string: String) = string.trim match {
    case "" => None
    case string => Some(string)
  }

  private def resolveDir(path : Option[Path]) : Option[Directory] = {
    path match {
      case None => None
      case Some(x : Path) =>  {
        Some(x.toDirectory)
      }
    }
  }

  private def resolvePath(path: Option[String]): Option[Path] = {
    path match {
      case None => None
      case Some(path:String) => {
        val rawPath = Path(path)
        if (rawPath.isAbsolute) Some(path) else Some(YANKEEDO_HOME / path)
      }
    }

  }
}

