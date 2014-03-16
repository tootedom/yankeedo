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

import scala.util.Random
import akka.camel.CamelMessage
import collection.mutable.ArrayBuffer

/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 20:23
 */
case object ONE_KILOBYTE_RANDOM_STRING extends RandomStringMessageSource(1024);
case object TEN_KILOBYTE_RANDOM_STRING extends RandomStringMessageSource(10240);

class RandomStringMessageSource(val byteLength : Int = 1024 ) extends CamelMessageSource {

  val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray

  val length = alphabet.length


  def getMessage: CamelMessage = {
    val random = new Random();
    val buffer = new ArrayBuffer[Byte](byteLength)
    for (i <- 0 to byteLength) {
      buffer += alphabet(random.nextInt(length)).toByte
    }

    val string = new String(buffer.toArray,"ASCII")
    CamelMessage(string,Map.empty)
  }
}
