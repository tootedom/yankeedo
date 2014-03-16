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

import akka.actor.ActorSystem
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.specs2.specification.BeforeAfter

/**
 * Created by dominictootell on 14/03/2014.
 */
case class WithActorSystem() extends BeforeAfter {
  val int : AtomicInteger = new AtomicInteger(0)
  @volatile var latch : CountDownLatch = null
  @volatile var actorSystem : ActorSystem = null

  def before = {
    latch = new CountDownLatch(1)
    actorSystem = ActorSystem("TestSystem"+int.incrementAndGet())
  }
  def after = {
    try {
      actorSystem.shutdown()
      actorSystem.awaitTermination()
    } catch {
      case e : Exception => {

      }
    }
  }
}
