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
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.specs2.mutable.Specification

/**
 * Created by dominictootell on 16/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class FileBasedMessageSourceSpec extends Specification {
  val testDirectoryPath =  this.getClass.getResource("files/test.json").getPath
  val nonExistentTestDirectoryPath =  Path("files/test-not-exists.json")

  "A DirectoryBasedMessageSource" should {
    "When given a file, the file name should be used in the message" in {

      val directorySource = new FileBasedMessageSource(path = testDirectoryPath,

        sendFilesAsBytes = false, messageHeaders = Some( (file : Option[File]) => {

          file match {
            case Some(f) => {
              Map( "FileName" -> f.name, "Last-Modified" -> f.lastModified,
                "SystemCurrentTimeInMillis" -> System.currentTimeMillis())
            }
            case None => {
              Map("SystemCurrentTimeInMillis" -> System.currentTimeMillis())
            }
          }


        }))

      val message = directorySource.getMessage

      message.getHeaders.get("FileName") must be equalTo("test.json")

      message.body should be equalTo("{\"hello\":\"world\"}")

      val message2 = directorySource.getMessage

      message2.getHeaders.get("FileName") must be equalTo("test.json")



    }
    "When given a non existent file, the message headers should not have filename" in {
      val directorySource = new FileBasedMessageSource(path = nonExistentTestDirectoryPath,

        sendFilesAsBytes = false, messageHeaders = Some( (file : Option[File]) => {

          file match {
            case Some(f) => {
              Map( "FileName" -> f.name, "Last-Modified" -> f.lastModified,
                "SystemCurrentTimeInMillis" -> System.currentTimeMillis())
            }
            case None => {
              Map("SystemCurrentTimeInMillis" -> System.currentTimeMillis())
            }
          }


        }))

      val message = directorySource.getMessage

      message.getHeaders.get("FileName") must beNull
      message.getHeaders.containsKey("SystemCurrentTimeInMillis") should beTrue




    }
  }
}
