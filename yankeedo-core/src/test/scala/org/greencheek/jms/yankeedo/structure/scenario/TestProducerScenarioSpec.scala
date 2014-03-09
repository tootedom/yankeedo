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

import java.util.concurrent.{TimeUnit, CountDownLatch}
import akka.actor.{ActorRef, Props, ActorSystem}
import org.greencheek.jms.yankeedo.scenarioexecution.{StartExecutingScenarios, ScenariosExecutionManager}
import scala.concurrent.duration._
import org.specs2.mutable.Specification
import org.apache.activemq.broker.BrokerService
import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
import org.greencheek.jms.util.PortUtil
import org.specs2.specification.{BeforeAfter}
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import org.apache.activemq.broker.jmx.ManagementContext


/**
 * Created by dominictootell on 09/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class TestProducerScenarioSpec extends Specification {

  case class withBroker() extends BeforeAfter {
    val broker : BrokerService = new BrokerService();
    broker.setPersistent(false)
    broker.setUseJmx(true)
    val mctx = new ManagementContext(java.lang.management.ManagementFactory.getPlatformMBeanServer)
    mctx.setCreateMBeanServer(false)
    mctx.setCreateConnector(false)
    mctx.setUseMBeanServer(true)
    broker.setManagementContext(mctx)

    val port : Int = PortUtil.findFreePort
    var appLatch : CountDownLatch = null
    var actorSystem : ActorSystem = null
    var scenarioExecutor : ActorRef = null

    def before = {
      broker.setPersistent(false)
      broker.start()

      appLatch = new CountDownLatch(1)
      actorSystem = ActorSystem()
    }
    def after  = {
      broker.stop()
      actorSystem.shutdown()
    }
  }

  "Producing messages" should {

    implicit val myContext = new withBroker()

    "terminate after 10 messages" >> {
      val scenario = createScenario(
        "produce 10 message scenario" connect_to "vm://localhost:" +  myContext.port
          until_no_of_messages_sent 10
          produce to queue "queue"
      )

      myContext.scenarioExecutor = myContext.actorSystem.actorOf(Props(new ScenariosExecutionManager(myContext.appLatch,ScenarioContainer(scenario))))
      myContext.scenarioExecutor ! StartExecutingScenarios

      var ok : Boolean = false
      try {
        ok = myContext.appLatch.await(10,TimeUnit.SECONDS)
      } catch {
        case e: Exception => {

        }
      }

      ok should beTrue

      myContext.broker.getAdminView.getTotalMessageCount should beEqualTo(10)
      myContext.broker.getAdminView.getTotalProducerCount should beEqualTo(0)



    }
  }
}
