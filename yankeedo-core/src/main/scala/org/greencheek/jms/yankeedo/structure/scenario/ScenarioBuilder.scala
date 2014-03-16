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
package org.greencheek.jms.yankeedo.structure.scenario

import java.util.concurrent.TimeUnit
import org.greencheek.jms.yankeedo.structure.actions.JmsAction
import org.greencheek.jms.yankeedo.config.JmsConfiguration

import concurrent.duration.{Duration}
import org.greencheek.jms.yankeedo.structure.RequiredParameter._

/**
 * User: dominictootell
 * Date: 01/01/2013
 * Time: 18:15
 */


object ScenarioBuilder {
  val DEFAULT_NUMBER_OF_MESSAGES_TO_SEND = -1;  // infinate
  val DEFAULT_NUMBER_OF_ACTORS = 1;
  val DEFAULT_DURATION_OF_SCENARIO = Duration.Inf


  class ScenarioBuilder[HASJMSURL,HASACTION,HASNAME](val duration :  Duration = DEFAULT_DURATION_OF_SCENARIO,
                                   val messages : Long = DEFAULT_NUMBER_OF_MESSAGES_TO_SEND,
                                   val numberOfActors : Int = DEFAULT_NUMBER_OF_ACTORS,
                                   val jmsurl : Option[String],
                                   val jmsAction : Option[JmsAction],
                                   val jmsConfiguration : Option[JmsConfiguration],
                                   val scenarioName : String)
  {
    def named(name : String) = new ScenarioBuilder[HASJMSURL,HASACTION,TRUE](duration,messages,numberOfActors,jmsurl,
                                                                        jmsAction,jmsConfiguration,name)

    def runForDuration(millis : Duration)  = new ScenarioBuilder[HASJMSURL,HASACTION,HASNAME](millis,messages,numberOfActors,jmsurl,
                                                                                  jmsAction,jmsConfiguration,scenarioName)
    def runForMessages(messages : Long) = new ScenarioBuilder[HASJMSURL,HASACTION,HASNAME](duration,messages,numberOfActors,jmsurl,
                                                                                   jmsAction,jmsConfiguration,scenarioName)
    def runWithConcurrency(concurrency : Int) = new ScenarioBuilder[HASJMSURL,HASACTION,HASNAME](duration,messages,concurrency,jmsurl,
                                                                                        jmsAction,jmsConfiguration,scenarioName)
    def withConnectionUrl(connectionUrl : String) = new ScenarioBuilder[TRUE,HASACTION,HASNAME](duration,messages,numberOfActors,Some(connectionUrl),
                                                                                        jmsAction,jmsConfiguration,scenarioName)
    def withJmsAction(jmsAction : JmsAction) = new ScenarioBuilder[HASJMSURL, TRUE,HASNAME](duration,messages,numberOfActors,jmsurl,
      Some(jmsAction),jmsConfiguration,scenarioName)
  }

  implicit def enableBuild(builder:ScenarioBuilder[TRUE,TRUE,TRUE]) = new {
    def build() =
      new Scenario(builder.duration, builder.messages, builder.numberOfActors, builder.jmsurl.get,builder.jmsAction.get,builder.jmsConfiguration,builder.scenarioName);
  }

  def builder(scenarioName : String) = new ScenarioBuilder[FALSE,FALSE,TRUE](scenarioName = scenarioName,jmsurl = None, jmsAction = None, jmsConfiguration = None)

}
