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
package org.greencheek.jms.yankeedo.structure

import actions.JmsActionTypeBuilder
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import actions.{JmsProducerAction,JmsConsumerAction}

/**
 * User: dominictootell
 * Date: 01/01/2013
 * Time: 22:35
 */
@RunWith(classOf[JUnitRunner])
class JmsActionTypeBuilderSpec extends Specification {

  "when builder is instructed to consumeQueue it" should {
    "create a Consumer JmsAction" in {
      JmsActionTypeBuilder.builder consumeQueue("dom") build() must haveClass[JmsConsumerAction]
    }



    //
    //    scenariobuilder consumer consume(queue("xxx")|topic("xxx")|durableTopic("xxxx","subscriptionName"))
    //    scenariobuilder producer sendto(queue("xxx")|topic("kkk"))
    //
    //    consumer runFor(10 seconds) runFor(10 messages)
    //    producer runFor(10 seconds) runFor(10 messages)


  }
}
