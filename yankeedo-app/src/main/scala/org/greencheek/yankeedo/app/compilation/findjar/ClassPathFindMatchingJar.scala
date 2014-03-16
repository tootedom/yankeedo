/**
 * Copyright 2012-2014 greencheek.org (www.greencheek.org)
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
package org.greencheek.yankeedo.app.compilation.findjar

import java.io.File
import java.net.URLClassLoader

/**
  * User: dominictootell
  * Date: 03/01/2013
  * Time: 22:12
  */
class ClassPathFindMatchingJar extends FindMatchingJar {

   val classpathURLs = Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader].getURLs

   def findJarMatching(regex: String): File = {
     val compiledRegex = regex.r
     val jarUrl = classpathURLs
       .filter(url => compiledRegex.findFirstMatchIn(url.toString).isDefined)
       .headOption
       .getOrElse(throw new RuntimeException("Can't find the jar matching " + regex))

     new File(jarUrl.toURI)
   }

 }
