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
package org.greencheek.jms.yankeedo.structure.scenario

import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import akka.actor.{Props, ActorRef}
import org.greencheek.jms.yankeedo.scenarioexecution.{StartExecutingScenarios, ScenariosExecutionManager}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 25/03/2014.
 */
class TestAsyncProducerScenarioSpec extends BrokerBasedSpec {


  val myContext = WithActorSystem();


  "Producing messages" >> {
    "terminate after 10 messages, presenting the timings" in myContext {
      val appLatch = myContext.latch
      val actorSystem = myContext.actorSystem

      val scenario = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" + port + "?daemon=true&jms.closeTimeout=200"
          until_no_of_messages_sent 10
          produce to queue "asyncqueue"
          with_no_broker_ack and
          with_per_message_delay_of(Duration(200,TimeUnit.MILLISECONDS))
      )

      val scenarioExecutor: ActorRef = actorSystem.actorOf(Props(new ScenariosExecutionManager(appLatch, ScenarioContainer(scenario).outputStats())))
      scenarioExecutor ! StartExecutingScenarios

      var ok: Boolean = false
      try {
        ok = appLatch.await(30, TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }

      ok should beTrue

      val map = broker.getBroker.getDestinationMap()
      getMessageCountForQueueDestination(map, "asyncqueue") should beEqualTo(10)
      getProducerCountForQueueDestination(map, "asyncqueue") should beEqualTo(0)

    }
  }

}
