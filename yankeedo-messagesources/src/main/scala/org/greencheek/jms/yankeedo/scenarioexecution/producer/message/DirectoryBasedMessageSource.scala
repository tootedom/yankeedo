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
package org.greencheek.jms.yankeedo.scenarioexecution.producer.message

import akka.camel.CamelMessage
import reflect.io.{File, Path}
import grizzled.slf4j.Logging
import javax.xml.soap.SOAPMessage
import java.util.concurrent.atomic.AtomicInteger

/**
 * User: dominictootell
 * Date: 20/01/2013
 * Time: 23:05
 */
object DirectoryBasedMessageSource {
  def SORT_BY_NAME_CASE_INSENSITIVE = { Some((file1:File,file2:File) => file1.name.compareToIgnoreCase(file2.name) < 0) }
  def SORT_BY_NAME_CASE_SENSITIVE = { Some((file1:File,file2:File) => file1.name.compare(file2.name) < 0)}
  def SORT_BY_FILE_LENGTH_LARGEST_FIRST = { Some((file1:File,file2:File) => file1.length >= file2.length) }
  def SORT_BY_FILE_LENGTH_SMALLEST_FIRST = { Some((file1:File,file2:File) => file1.length <= file2.length) }
  def SORT_BY_LAST_MODIFIED_DATE_NEW_FIRST = { Some((file1:File,file2:File) => file1.isFresher(file2) )}
  def SORT_BY_LAST_MODIFIED_DATE_NEW_LAST = { Some((file1:File,file2:File) => file1.lastModified < file2.lastModified )}

  def FILTER_BY_EXTENSION = { (extension:String) => Some((file:File) => file.hasExtension(extension))}
  def FILTER_BY_NOT_EXTENSION = { (extension:String) => Some((file:File) => !file.hasExtension(extension))}
}


class DirectoryBasedMessageSource(val path : Path,
                                  val sendFilesAsBytes : Boolean = true,
                                  val sortOrder : Option[(File,File) => Boolean] = DirectoryBasedMessageSource.SORT_BY_NAME_CASE_INSENSITIVE,
                                  val filter : Option[(File) => Boolean] = None,
                                  val messageHeaders : Option[(File) => Map[String,Any]] = None
                                  ) extends CamelMessageSource with Logging {

  //path.toDirectory.files.toList
  private val random = sortOrder match {
    case None => true
    case Some(x) => false
  }

  val files = filter match {
    case None => getFiles
    case Some(fileFilter) => {
      getFiles.filter { fileFilter(_) }
    }
  }

  private val numberOfFiles = files.size

  if (numberOfFiles == 0) throw new InstantiationError("No files in directory " + path + " that match filter")

  // Protected by synchronised below
  private val currentFileNumber = new AtomicInteger(0);

  def numberOfFilesAvailable = numberOfFiles

  private def findNextPositivePowerOfTwo(value : Int) : Int =
  {
    return 1 << (32 - Integer.numberOfLeadingZeros(value-1));
  }

  /**
   * If the list of files in the directory is to be sorted,
   * then we sort the list.  Otherwise we just return the list of
   * files as they are read from the dir
   * @return
   */
  private def getFiles : List[File] = {
      random match {
        case true => path.toDirectory.files.toList
        case false => {
          path.toDirectory.files.toList.sortWith(sortOrder.get)
        }
      }
  }

  def getMessage: CamelMessage = {
    val file = random match {
      case true =>  files(0 + (Math.random().toInt * (numberOfFiles)))
      case false => {
        files(getAndIncrement)
      }
    }

    val headers : Map[String,Any] = messageHeaders match {
      case None => Map.empty
      case Some(function) => function(file)
    }

    sendFilesAsBytes match {
      case true => CamelMessage(readFileAsBytes(file),headers)
      case false => CamelMessage(readFileAsString(file),headers)
    }

  }


  private def getAndIncrement : Int = {
    val currentFile = currentFileNumber.getAndIncrement
    currentFile % numberOfFiles;
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
