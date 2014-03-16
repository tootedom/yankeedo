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
package org.greencheek.jms.yankeedo.structure.actions

/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 15:17
 */
object JmsDestination {
  import java.util.concurrent.atomic.AtomicInteger

  private val durableTopicSubscripberNameIncrementer = new AtomicInteger(1)
  private val durableTopicClientIdIncrementer = new AtomicInteger(1)

  val defaultTopicName : String = "yankeedoo.topic"
  val defaultQueueName : String = "yankeedoo.queue"
  val defaultDurableTopicName : String = "yankeedoo.durabletopic"

  def defaultDurableTopicSubscriptionName : String = "yankeedoo.subscription."+durableTopicSubscripberNameIncrementer.getAndIncrement
  def defaultDurableTopicClientId : String = "yankeedoo.client."+durableTopicClientIdIncrementer.getAndIncrement
}


abstract class JmsDestination {
  val name: String
}
case class Queue(val name: String = JmsDestination.defaultQueueName) extends JmsDestination {

  override def toString = {
    val buf = new StringBuilder
    buf ++= "queue:" ++= name
    buf.toString()
  }
}

case class Topic(val name: String = JmsDestination.defaultDurableTopicName) extends JmsDestination {
  override def toString = {
    val buf = new StringBuilder
    buf ++= "topic:" ++= name
    buf.toString()
  }
}

case class DurableTopic(override val name: String = JmsDestination.defaultDurableTopicName,
                        val subscriptionName: String = JmsDestination.defaultDurableTopicSubscriptionName,
                        val clientId : String = JmsDestination.defaultDurableTopicClientId) extends JmsDestination {
  override def toString = {
    val buf = new StringBuilder
    buf ++= "durable-topic:" ++= name +=','
    buf ++= "subscriptionName:" ++=subscriptionName +=','
    buf ++= "clientId:" ++= clientId
    buf.toString()
  }
}

