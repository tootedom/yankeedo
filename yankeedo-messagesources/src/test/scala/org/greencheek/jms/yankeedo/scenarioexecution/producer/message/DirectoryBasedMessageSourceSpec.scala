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

import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import reflect.io.File
import scala.concurrent.{Future, Await, ExecutionContext}
import java.util.concurrent.{CopyOnWriteArrayList, ConcurrentSkipListSet}
import scala.concurrent.duration._


/**
 * User: dominictootell
 * Date: 01/01/2013
 * Time: 17:10
 */
@RunWith(classOf[JUnitRunner])
class DirectoryBasedMessageSourceSpec extends Specification {
  val testDirectoryPath =  File(this.getClass.getResource("directory-for-dir-message-source/A").getPath).parent.path
  val jpgOneSize = File(this.getClass.getResource("directory-for-dir-message-source/ducks.jpg").getPath).length.toInt
  val jpgTwoSize = File(this.getClass.getResource("directory-for-dir-message-source/IMAG1365.jpg").getPath).length.toInt

  def excludeJPGs(file : File) = {
    !file.hasExtension("jpg")
  }

  def includeJPGs(file : File) = {
    file.hasExtension("jpg")
  }

  "A DirectoryBasedMessageSource" should {
    "Should return a file from a directory" in {

      val directorySource = new DirectoryBasedMessageSource(testDirectoryPath)
      directorySource.getMessage must not be null


    }
    "Should return a bytes message from a file in a directory" in {

      val directorySource = new DirectoryBasedMessageSource(testDirectoryPath)
      val message = directorySource.getMessage
      message must not be null
      message.body must haveClass[Array[Byte]]
    }
    "Should return a text message from a file in a directory" in {



      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
                                                            filter = (Some(excludeJPGs)),
                                                            sendFilesAsBytes = false)
      val message = directorySource.getMessage
      message must not be null
      message.body must haveClass[String]
    }
    "When a filter is applied the number of files available match that filter are used" in {
      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = (Some(excludeJPGs)),
        sendFilesAsBytes = false)

      directorySource.numberOfFilesAvailable must be equalTo 5
    }
    "When a filter is applied, to obtain only text files, the number of files available is looped through " in {
      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = Some( (file) => file.name.equals("A") || file.name.equals("c")  ),
        sendFilesAsBytes = false)

      directorySource.numberOfFilesAvailable must be equalTo 2

      var firstText = ""
      var secondText = ""
      var lastText = ""
      for (x <- 1 to 6) {
        x match {
          case 1 => firstText = directorySource.getMessage.body.asInstanceOf[String]
          case 3 => secondText = directorySource.getMessage.body.asInstanceOf[String]
          case 5 => lastText = directorySource.getMessage.body.asInstanceOf[String]
          case _ => directorySource.getMessage
        }
      }

      firstText must be equalTo secondText
      secondText must be equalTo lastText
      firstText must be equalTo lastText
    }
    "The sort order is by default ordered by the unicode value of the lowercase values of the chars in the string" in {
      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
        sendFilesAsBytes = false)

      directorySource.getMessage
      directorySource.getMessage.body.asInstanceOf[String] must be equalTo("c")
    }
    "When a sort order is applied, to order by name that based on the Unicode value of the chars in the string, the first file is A" in {
      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
        sendFilesAsBytes = false,
        sortOrder = (Some( { (file1,file2) => file1.name.compareTo(file2.name) < 0 })))

      directorySource.getMessage
      directorySource.getMessage.body.asInstanceOf[String] must be equalTo("D")
    }
    "When a filter is applied, to obtain only jpgs, the returned message size is that of the jpg sizes" in {
      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = (Some(includeJPGs)),
        sendFilesAsBytes = true, sortOrder = (Some( _.length < _.length )))

      val sizes = Array(directorySource.getMessage.body.asInstanceOf[Array[Byte]].length,
      directorySource.getMessage.body.asInstanceOf[Array[Byte]].length).sortWith( _ < _ )

      sizes must be equalTo Array(jpgOneSize,jpgTwoSize)

    }
    "When using the provided filter, I can filter by extension 'jpg'" in {
      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = DirectoryBasedMessageSource.FILTER_BY_EXTENSION("jpg"),
        sendFilesAsBytes = true)

      directorySource.numberOfFilesAvailable must be equalTo 2

      val directorySourceText = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = DirectoryBasedMessageSource.FILTER_BY_EXTENSION("text"),
        sendFilesAsBytes = true)


      directorySourceText.numberOfFilesAvailable must be equalTo 1

    }
    "When a sort order is not specified, the files are read in a random order" in {
      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = DirectoryBasedMessageSource.FILTER_BY_NOT_EXTENSION("jpg"),
        sortOrder = None,
        sendFilesAsBytes = false)

      val files = directorySource.files
      var messageContents : List[String] = Nil
      var filesContents : List[String] = Nil

      files.foreach { (file:File) =>
        filesContents = file.slurp() :: filesContents
        messageContents = directorySource.getMessage.body.asInstanceOf[String] :: messageContents
      }

      messageContents must not be equalTo(filesContents)
    }
    "When a directory is sorted by last modified, the files are returned in the correct order" in {
      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = DirectoryBasedMessageSource.FILTER_BY_NOT_EXTENSION("jpg"),
        sortOrder = DirectoryBasedMessageSource.SORT_BY_LAST_MODIFIED_DATE_NEW_FIRST,
        sendFilesAsBytes = false)

      val files = directorySource.files

      files.foreach { (file:File) =>
        file.lastModified_=(System.currentTimeMillis())
        Thread.sleep(1000)
      }

      val directorySourceRecentFirst = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = DirectoryBasedMessageSource.FILTER_BY_NOT_EXTENSION("jpg"),
        sortOrder = DirectoryBasedMessageSource.SORT_BY_LAST_MODIFIED_DATE_NEW_FIRST,
        sendFilesAsBytes = false)

      val directorySourceRecentLast = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = DirectoryBasedMessageSource.FILTER_BY_NOT_EXTENSION("jpg"),
        sortOrder = DirectoryBasedMessageSource.SORT_BY_LAST_MODIFIED_DATE_NEW_LAST,
        sendFilesAsBytes = false)

      val orderedByMostRecent = directorySourceRecentFirst.files
      val orderedByLeastRecent = directorySourceRecentLast.files


      files.reverse should be equalTo(orderedByMostRecent)
      files should not be equalTo(orderedByMostRecent)

      files.reverse should not be equalTo(orderedByLeastRecent)
      orderedByMostRecent.reverse should be equalTo(orderedByLeastRecent)

    }
    "When given a list of attributes to add as message headers, the attributes are added to the message" in {

      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = Some( _.name.matches("[d|D]\\.t[e]?xt")),
        sortOrder = DirectoryBasedMessageSource.SORT_BY_NAME_CASE_SENSITIVE,
        sendFilesAsBytes = false, messageHeaders = Some( (file:File) => {
          Map( "FileName" -> file.name, "Last-Modified" -> file.lastModified)
        }))

      val message = directorySource.getMessage

      message.getHeaders.get("FileName")  must be equalTo("D.text")

      val message2 = directorySource.getMessage

      message2.getHeaders.get("FileName") must be equalTo("d.txt")

    }
    "When a directory is sorted by file size, the files are returned in order of their size" in {
      val directorySourceSortByLargest = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = Some(_.extension.matches("jpg")),
        sortOrder = DirectoryBasedMessageSource.SORT_BY_FILE_LENGTH_LARGEST_FIRST,
        sendFilesAsBytes = true, messageHeaders = Some( (file:File) => {
          Map( "FileName" -> file.name, "Last-Modified" -> file.lastModified, "FileLength" -> file.length)
        }))


      val message = directorySourceSortByLargest.getMessage
      val message2 = directorySourceSortByLargest.getMessage

      message.getHeaders.get("FileLength").asInstanceOf[Long] must be greaterThan message2.getHeaders.get("FileLength").asInstanceOf[Long]

      val directorySourceSortBySmallest = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = Some(_.extension.matches("jpg")),
        sortOrder = DirectoryBasedMessageSource.SORT_BY_FILE_LENGTH_SMALLEST_FIRST,
        sendFilesAsBytes = true, messageHeaders = Some( (file:File) => {
          Map( "FileName" -> file.name, "Last-Modified" -> file.lastModified, "FileLength" -> file.length)
        }))

      val message3 = directorySourceSortBySmallest.getMessage
      val message4 = directorySourceSortBySmallest.getMessage

      message3.getHeaders.get("FileLength").asInstanceOf[Long] must be lessThan message4.getHeaders.get("FileLength").asInstanceOf[Long]


    }
    "When multiple threads use a directory source all the files in the directory are read" in {
      implicit val ec = ExecutionContext.global

      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = DirectoryBasedMessageSource.FILTER_BY_NOT_EXTENSION("jpg"),
        sortOrder = DirectoryBasedMessageSource.SORT_BY_NAME_CASE_SENSITIVE,
        sendFilesAsBytes = false, messageHeaders = Some( (file:File) => {
          Map( "FileName" -> file.name, "Last-Modified" -> file.lastModified)
        }))

      val listOfFileContents = new CopyOnWriteArrayList[String]()

      val listOfFutures = List.fill(10) (
        Future {
          listOfFileContents.add(directorySource.getMessage.body.asInstanceOf[String])
        }
      )

      val executedFutures = Future.sequence(listOfFutures)

      Await.result(executedFutures,Duration("10 seconds"))

      listOfFileContents.size() must be equalTo 10

//      val list = List.fill(10003) (
//        Future {
//          for(i <- 1 to 100005)
//            counter2.increment
//        }
//      )
//
//      val executed2 = Future.sequence(list2)
//
//      Await.result(executed2,120 seconds)
    }
  }


}
