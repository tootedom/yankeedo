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

import tools.nsc.io.{Directory, Path}
import java.util.{HashMap => JMap}
import org.greencheek.yankeedo.app.ConfigConstants._
import scala.collection.JavaConversions

/**
 * User: dominictootell
 * Date: 19/01/2013
 * Time: 15:56
 */
object YankeedoCommandLineArgs {


  private val props : JMap[String,AnyRef] = new JMap()


  def sourcesDirectory(v: String) {
    props.put(CONF_DIRECTORY_SCENARIOS,v)
  }

  def binariesDirectory(v: String) {
    props.put(CONF_DIRECTORY_BINARIES,v)
  }

  def runScenarioWithName(v: String) {
    props.put(CONF_SCENARIO_NAME_TO_RUN,v)
  }


  def build = props
}

