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
package org.greencheek.jms.yankeedo.app

import org.greencheek.jms.yankeedo.structure.scenario.{ScenarioBuilder, ScenarioContainer}
import org.greencheek.jms.yankeedo.structure.actions.{JmsProducerAction, JmsActionTypeBuilder}
import java.util.concurrent.CountDownLatch
import akka.actor.{Props, ActorSystem}
import scala.concurrent.duration._
import org.greencheek.jms.yankeedo.scenarioexecution.{StartExecutingScenarios, ScenariosExecutionManager}
import org.greencheek.jms.yankeedo.structure.scenario

/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 15:41
 *
 * Not to be used, just an example for quick testing.
 */
object AmqJmsLine extends App {

  class MySimpleTestScenarioContainer extends ScenarioContainer {
    withScenarios(
      List(
//        ScenarioBuilder.builder.withConnectionUrl("tcp://localhost:61616?daemon=true") runForMessages(2) runWithConcurrency 2 runForDuration (60000 milliseconds) withJmsAction( JmsActionTypeBuilder.builder.consumeQueue("dom") build() ) build(),
//        ScenarioBuilder.builder.withConnectionUrl("tcp://localhost:61616?daemon=true") runForMessages(10) withJmsAction( JmsActionTypeBuilder.builder.consumeTopic("dom2") build()) build(),
//
//        ScenarioBuilder.builder.withConnectionUrl("tcp://localhost:61616?daemon=true") runForMessages(100) withJmsAction( JmsActionTypeBuilder.builder.sendToQueue("domff") build()) build(),
//        ScenarioBuilder.builder.withConnectionUrl("tcp://localhost:61616?daemon=true") runForMessages(100) runWithConcurrency 4 withJmsAction( (JmsActionTypeBuilder.builder.sendToQueue("domf2") build()).asInstanceOf[JmsProducerAction] sendMessageWithDelayOf(1 second) ) build()



      )
    )
    runFor(120000 milliseconds)
  }

  val scenariosToExecute = new MySimpleTestScenarioContainer



  val appLatch = new CountDownLatch(1)
  val actorSystem = ActorSystem()

  val scenarioExecutor = actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch,scenariosToExecute)))

  scenarioExecutor ! StartExecutingScenarios

  appLatch.await()

  actorSystem.shutdown()


}
