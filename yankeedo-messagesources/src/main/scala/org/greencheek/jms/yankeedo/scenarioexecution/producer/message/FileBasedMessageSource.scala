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
package org.greencheek.jms.yankeedo.scenarioexecution.producer.message

import scala.reflect.io.{Path, File}
import akka.camel.CamelMessage
import grizzled.slf4j.Logging

/**
 * Created by dominictootell on 16/03/2014.
 */
class FileBasedMessageSource(val path : Path,
                             val sendFilesAsBytes : Boolean = true,
                             val messageHeaders : Option[(Option[File]) => Map[String,Any]] = None,
                             val fileNotFoundContent : Option[Array[Byte]] = None)
  extends CamelMessageSource with Logging
{

  val fileContent : (Any,Option[File]) = getFileContent(path,sendFilesAsBytes,fileNotFoundContent)

  private def getFileContent(path : Path,
                             sendFilesAsBytes : Boolean,
                             fileNotFoundContent : Option[Array[Byte]]) : (Any,Option[File]) = {
    if(path.canRead) {
      try {

        val content : Any = sendFilesAsBytes match
        {
          case true => {
            readFileAsBytes(path.toFile)
          }
          case false => {
            readFileAsString(path.toFile)
          }
        }

        return (content,Some(path.toFile))

      } catch {
        case e : Exception => {
          return (fileNotFoundContent,None)
        }
      }
    } else {
      return (fileNotFoundContent,None)
    }
  }

  def getMessage: CamelMessage = {


    val headers : Map[String,Any] = messageHeaders match {
      case None => Map.empty
      case Some(function) => {
        function(fileContent._2)
      }
    }

    sendFilesAsBytes match {
      case true => CamelMessage(fileContent._1,headers)
      case false => CamelMessage(fileContent._1,headers)
    }

  }

  def readFileAsBytes(file : File) : Array[Byte] = {
    file.toByteArray()
  }

  def readFileAsString(file : File) : String = {
    try {
      file.slurp()
    } catch {
      case e : Exception => {
        warn("Unable to read file: " + file.path + " as a String: ",e)
      }
        ""
    }
  }
}
