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

import java.io.{UnsupportedEncodingException, File}
import java.net.{URLDecoder, URLClassLoader}
import java.nio.charset.Charset

/**
  * User: dominictootell
  * Date: 03/01/2013
  * Time: 22:12
  */

object ClassPathOrShadeMatchingJarFinder {

  val currentClassIsLocatedAt : Option[File]  = findClassLocation
  val isClassContainedInShadedJar = isCurrentClassInShade

  private def isCurrentClassInShade : Boolean = {
    currentClassIsLocatedAt match {
      case None => false
      case Some(x: File) => {
        x.getAbsolutePath.contains("shade.jar")
      }
    }
  }

  private def findClassLocation : Option[File] = {

    val uri = this.getClass.getProtectionDomain().getCodeSource().getLocation.toExternalForm;


    if (uri.startsWith("file:")) {
      val fileName = URLDecoder.decode(uri.substring("file:".length()), Charset.defaultCharset().name());
      return Some(new File(fileName));
    }

    if (!uri.startsWith("jar:file:")) {
      return None
    } else {
      val idx = uri.indexOf('!');
      try {
        val fileName = URLDecoder.decode(uri.substring("jar:file:".length(), idx), Charset.defaultCharset().name());
        return Some(new File(fileName))
      } catch {
        case e: UnsupportedEncodingException => throw new InternalError("default charset doesn't exist. Your VM is borked.");
      }
    }
  }

}

class ClassPathOrShadeMatchingJarFinder extends FindMatchingJar {

   val classpathURLs = Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader].getURLs
   val regexp = """(.*amqjmsline-app-\d+\.\d+\.\d+.*-shade.jar)$""";


   def findJarMatching(regex: String): File = {
     ClassPathOrShadeMatchingJarFinder.isClassContainedInShadedJar match {
       case true => ClassPathOrShadeMatchingJarFinder.currentClassIsLocatedAt.get
       case false => {
         val compiledRegex = regex.r
         val jarUrl = classpathURLs
           .filter(url => compiledRegex.findFirstMatchIn(url.toString).isDefined)
           .headOption
           .getOrElse(throw new RuntimeException("Can't find the jar matching " + regex))

         new File(jarUrl.toURI)
       }
     }
   }
 }
