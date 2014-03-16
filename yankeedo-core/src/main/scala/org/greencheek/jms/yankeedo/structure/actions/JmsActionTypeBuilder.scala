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
package org.greencheek.jms.yankeedo.structure.actions

import org.greencheek.jms.yankeedo.structure.RequiredParameter.{FALSE, TRUE}


/**
 * User: dominictootell
 * Date: 01/01/2013
 * Time: 21:22
 */
object JmsActionTypeBuilder {

  sealed abstract class ActionType
  case object PRODUCER extends ActionType
  case object CONSUMER extends ActionType

  class JmsActionTypeBuilder[HAS_DESTINATION_TYPE](val destination : Option[JmsDestination],
                                                   val actionType : Option[ActionType]
                                             )
  {
    def consumeQueue(dest : String) = new JmsActionTypeBuilder[TRUE](Some(new Queue(dest)),Some(CONSUMER))

    def consumeTopic(dest: String) =
      new JmsActionTypeBuilder[TRUE](Some(new Topic(dest)),Some(CONSUMER))

    def consumerDurableTopicWithSubscriptionName(dest: String, subscriptionName: String) = {
      new JmsActionTypeBuilder[TRUE](
        Some(new DurableTopic(dest,subscriptionName)),Some(CONSUMER))
    }

    def consumerDurableTopicWithSubscriptionAndClientId(dest: String, subscriptionName : String, clientId : String) = {
      new JmsActionTypeBuilder[TRUE](
        Some(new DurableTopic(dest,subscriptionName,clientId)),Some(CONSUMER))
    }


    def consumerDurableTopicWithClientId(dest: String,clientId:String) = {
      new JmsActionTypeBuilder[TRUE](
        Some(new DurableTopic(dest,clientId = clientId)),Some(CONSUMER))
    }

    def consumerDurableTopic(dest: String) = {
      new JmsActionTypeBuilder[TRUE](
        Some(new DurableTopic(dest)),Some(CONSUMER))
    }


    def sendToQueue(dest: String) =
      new JmsActionTypeBuilder[TRUE](Some(new Queue(dest)),Some(PRODUCER))

    def sendToTopic(dest: String) =
      new JmsActionTypeBuilder[TRUE](Some(new Topic(dest)),Some(PRODUCER))

  }

  implicit def enableBuild(builder:JmsActionTypeBuilder[TRUE]) = new {
    def build() = {
      builder.actionType.get match {
        case CONSUMER => {
          new JmsConsumerAction(builder.destination.get)
        }
        case PRODUCER => {
          new JmsProducerAction(builder.destination.get)
        }
      }
    }

  }

  def builder = new JmsActionTypeBuilder[FALSE](None,None)

}
