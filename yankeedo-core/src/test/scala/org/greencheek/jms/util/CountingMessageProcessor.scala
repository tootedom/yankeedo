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
package org.greencheek.jms.util

import akka.camel.CamelMessage
import org.greencheek.jms.yankeedo.consumer.scenarioexecution.messageprocessor.CamelMessageProcessor

/**
 * Created by dominictootell on 16/03/2014.
 */
class CountingMessageProcessor extends CamelMessageProcessor {
  @volatile var _numberOfPersistentMessages : Int = 0

  def process(message: CamelMessage) {
    val value = message.getHeaders.get("JMSDeliveryMode")
    if(value !=null) {
      if(value.equals(2)) {
        _numberOfPersistentMessages+=1
      }
    }
  }

  def consumerOnError: Boolean = true

  def numberOfPersistentMessages : Int = {
    _numberOfPersistentMessages
  }
}
