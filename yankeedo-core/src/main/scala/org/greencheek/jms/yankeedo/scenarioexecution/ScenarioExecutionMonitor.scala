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
package org.greencheek.jms.yankeedo.scenarioexecution

import akka.actor._
import org.greencheek.jms.yankeedo.config.{JmsConfiguration, ClassPathXmlApplicationContextJmsConfiguration}
import org.greencheek.jms.yankeedo.structure.scenario.Scenario
import akka.camel.{Camel, CamelExtension}
import scala.Some
import java.util.concurrent.atomic.{AtomicLong}
import scala.concurrent.duration._
import grizzled.slf4j.Logging
import akka.camel.internal.DefaultCamel
import scala.util.control.NonFatal
import org.apache.camel.Component

/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 22:08
 */
class ScenarioExecutionMonitor(val scenario : Scenario,
                               val children : (ActorContext, AtomicLong) => List[ActorRef]) extends Actor with Logging {

  val jmsComponent = scenario.jmsConfiguration match {
    case None => new ClassPathXmlApplicationContextJmsConfiguration(scenario)
    case Some(x: JmsConfiguration) => x
  }

  print("xxxxxxxxxx")
  print(context.system)
  val camel : Camel = CamelExtension(context.system)
  val camelContext = camel.context
  camelContext.addComponent("jms",jmsComponent.getJmsComponent())

  val numberOfMessagesAttemptedProcessing = new AtomicLong(scenario.numberOfMessages)

  // Start the children, using this as the parent, passing the number of messages that must be
  // processed by the child actors before they exit.  One of the child actors will send a
  // ConsumerFinished, or ParentFinished message
  val childrenActorRefs = children(context,numberOfMessagesAttemptedProcessing)

  val runForDuration : Option[Cancellable] = {
    scenario.runForDuration match {
      case duration:FiniteDuration => {
        import context.dispatcher
        Some(context.system.scheduler.scheduleOnce(scenario.runForDuration.asInstanceOf[FiniteDuration]) {
          self ! new ScenarioExecutionMonitorRunDurationFinished(scenario.runForDuration)
        })
      }
      case _ => {
        None
      }
    }
  }



  override def receive = {
    case ConsumerFinished => {
      doStopWithScheduledTimerStop()
    }
    case ProducerFinished => {
      doStopWithScheduledTimerStop()
    }
    case x: ScenarioExecutionMonitorRunDurationFinished => {
      doStop()
    }
  }

  private def doStopWithScheduledTimerStop() : Unit = {
    stopRunForDuration()
    doStop()
  }

  private def doStop() : Unit = {
    stopChildren()
    actorFinished()
    notifyParentScenarioHasFinished()
  }

  override def postStop() {
    info("Scenario finished:" + scenario)
  }

  private def stopRunForDuration() : Unit = {
    runForDuration match {
      case Some(x) => {
        x.cancel()
      }
      case _ => {}
    }
  }

  private def stopChildren() : Unit = {
    for (child : ActorRef <- childrenActorRefs) context.stop(child)
  }


  private def actorFinished() {
    debug("ExecutionMonitor for scenario is shutting down:" + scenario)

    debug("Stopping ExecutionMonitor actor")
    context.stop(self)

    debug("Stopping ExecutionMonitor's camel context")
    try {
//      val c : Component = camelContext.removeComponent("jms")
//      try camelContext.stop() finally {
//        try camel.template.stop() catch { case NonFatal(e) ⇒ debug("Swallowing non-fatal exception [{}] on stopping Camel producer template", e) }
//      }
//      debug("Stopped CamelContext[{}] for ActorSystem[{}]", camelContext.getName, context.system.name)
//      System.out.println("==============------------==============")
////      System.out.println(c)
//      System.out.println(camelContext)
//      System.out.println("==============------------==============")
    } catch {
      case e : Exception => {}
    }


    debug("Shutting down ExecutionMonitor's application context" + jmsComponent)
    jmsComponent.stop()


  }

  private def notifyParentScenarioHasFinished() {
    debug("ExecutionMonitor Messaging parent, of Termination")
    context.parent ! ExecutionMonitorFinished
  }
}
