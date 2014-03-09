package org.greencheek.jms.yankeedo.structure.scenario

import java.util.concurrent.CountDownLatch
import akka.actor.{Props, ActorSystem}
import org.greencheek.jms.yankeedo.scenarioexecution.{StartExecutingScenarios, ScenariosExecutionManager}
import scala.concurrent.duration._

/**
 * Created by dominictootell on 09/03/2014.
 */
class TestProducerScenarioSpec {

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
