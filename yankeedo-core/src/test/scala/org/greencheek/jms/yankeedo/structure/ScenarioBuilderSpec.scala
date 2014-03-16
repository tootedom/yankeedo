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
package org.greencheek.jms.yankeedo.structure

import actions.JmsActionTypeBuilder
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.greencheek.jms.yankeedo.structure
import scenario.{ScenarioBuilder, Scenario}
import scala.concurrent.duration._

/**
 * User: dominictootell
 * Date: 01/01/2013
 * Time: 17:10
 */
@RunWith(classOf[JUnitRunner])
class ScenarioBuilderSpec extends Specification {

  "A ScenarioBuilder" should {
    "require a jmsUrl to be specified" in {
      (ScenarioBuilder.builder("xxx") runForDuration(1000 milli) withConnectionUrl("tcp://localhost:61616")
        withJmsAction(JmsActionTypeBuilder.builder consumeQueue("dom") build()) build() must haveClass[Scenario])
    }
  }

  "A ScenarioBuilder with a duration" should {
    "be created when duration is specified" in {
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).duration must be equalTo (1000 milli)
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).messages must be equalTo (-1)
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).numberOfActors must be equalTo (1)
    }
    "be created when duration and messages is specified" in {
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).runForMessages(10).duration must be equalTo (1000 milli)
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).runForMessages(10).messages must be equalTo (10)
    }
    "be created when duration, messages and concurrency is specified" in {
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).runForMessages(10).runWithConcurrency(111).duration must be equalTo (1000 milli)
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).runForMessages(10).runWithConcurrency(111).messages must be equalTo (10)
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).runForMessages(10).runWithConcurrency(111).numberOfActors must be equalTo (111)
    }
    "be created when duration, messages, concurrency and url is specified" in {
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).runForMessages(10).withConnectionUrl("tcp://localhost:61616").runWithConcurrency(10).duration must be equalTo (1000 milli)
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).runForMessages(10).withConnectionUrl("tcp://localhost:61616").runWithConcurrency(105).messages must be equalTo (10)
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).runForMessages(10).withConnectionUrl("tcp://localhost:61616").runWithConcurrency(105).numberOfActors must be equalTo (105)
      ScenarioBuilder.builder("xxx").runForDuration(1000 milli).runForMessages(10).withConnectionUrl("tcp://localhost:61616").runWithConcurrency(105).jmsurl must be equalTo Some("tcp://localhost:61616")
    }
  }

}
