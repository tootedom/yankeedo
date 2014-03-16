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

import org.specs2.mutable.Specification
import org.apache.activemq.broker.BrokerService
import java.net.ServerSocket
import java.util.concurrent.ExecutorService
import org.greencheek.jms.util.PortUtil
import org.apache.activemq.store.memory.MemoryPersistenceAdapter
import java.util.{Map => JMap}
import org.apache.activemq.command.ActiveMQDestination
import org.apache.activemq.broker.region.{Subscription, DestinationStatistics, Destination}
import scala.collection.JavaConversions._

/**
 * Created by dominictootell on 14/03/2014.
 */

trait BrokerBasedSpec extends Specification {
  import org.specs2._
  import specification._

  @volatile var broker : BrokerService = null
  @volatile var portServerSocket : ServerSocket = null
  @volatile var port : Int = -1
  @volatile var es : ExecutorService = null

  /** the map method allows to "post-process" the fragments after their creation */
  override def map(fs: =>Fragments) = Step(startBroker) ^ fs ^ Step(stopBroker)

  def startBroker() = {
    {
      portServerSocket = PortUtil.findFreePort
      port = PortUtil.getPort(portServerSocket)
      println("==========")
      println("PORT:" + port)
      println("==========")
      broker = new BrokerService()
      broker.setBrokerName("localbroker")
      broker.setPersistent(false)
      broker.setUseJmx(false)

      broker.setPersistenceAdapter(new MemoryPersistenceAdapter())
      broker.addConnector("tcp://localhost:" + port);

      broker.setTmpDataDirectory(null)
      broker.setStartAsync(false)
      broker.setUseShutdownHook(false)
      broker.setUseVirtualTopics(false)
      broker.setAdvisorySupport(false)
      broker.setSchedulerSupport(false)
      broker.setDedicatedTaskRunner(false)
      broker.start()
      broker.waitUntilStarted()
    }
  }

  def stopBroker = {
    broker.stop()
    broker.waitUntilStopped()
  }


  def getConsumersForDestination(destinations : JMap[ActiveMQDestination,Destination],
                                        destinationName : String,
                                        filter : ((ActiveMQDestination) => Boolean)) : Option[List[Subscription]] = {

    for(dest : ActiveMQDestination <- destinations.keySet().toList) {
      if(filter(dest)) {
        if(dest.getPhysicalName.equals(destinationName)) {
          return Some(destinations.get(dest).getConsumers.toList)
        }
      }
    }
    None
  }

  def getDestinationStatsForDestination(destinations : JMap[ActiveMQDestination,Destination],
                                        destinationName : String,
                                        filter : ((ActiveMQDestination) => Boolean)) : Option[DestinationStatistics] = {

    for(dest : ActiveMQDestination <- destinations.keySet().toList) {
      if(filter(dest)) {
        if(dest.getPhysicalName.equals(destinationName)) {
          return Some(destinations.get(dest).getDestinationStatistics)
        }
      }
    }
    None
  }

  def getDestinationStatsForTopicDestination(destinations : JMap[ActiveMQDestination,Destination],
                                             destinationName : String) : Option[DestinationStatistics] = {
    getDestinationStatsForDestination(destinations,destinationName,{dest : ActiveMQDestination => dest.isTopic})
  }

  def getDestinationStatsForQueueDestination(destinations : JMap[ActiveMQDestination,Destination],
                                             destinationName : String) : Option[DestinationStatistics] = {
    getDestinationStatsForDestination(destinations,destinationName,{dest : ActiveMQDestination => dest.isQueue})
  }

  def getCountForDestination(destinations : JMap[ActiveMQDestination,Destination],
                             destinationName : String,
                             destinationFilter : (JMap[ActiveMQDestination,Destination], String) => Option[DestinationStatistics],
                             counterFuction : (DestinationStatistics) => Long ) : Long = {
    destinationFilter(destinations,destinationName) match {
      case None => 0
      case Some(d) => {
        counterFuction(d)
      }
    }
  }


  def getMessageCountForQueueDestination(destinations : JMap[ActiveMQDestination,Destination],
                                         destinationName : String) : Long = {
    getCountForDestination(destinations,destinationName,getDestinationStatsForQueueDestination(_,_),{ stat : DestinationStatistics => stat.getEnqueues.getCount - stat.getDequeues.getCount})
  }

  def getProducerCountForQueueDestination(destinations : JMap[ActiveMQDestination,Destination],
                                          destinationName : String) : Long = {
    getCountForDestination(destinations,destinationName,getDestinationStatsForQueueDestination(_,_),{ stat : DestinationStatistics => stat.getProducers.getCount})
  }

  def getMessageCountForTopicDestination(destinations : JMap[ActiveMQDestination,Destination],
                                         destinationName : String) : Long = {
    getCountForDestination(destinations,destinationName,getDestinationStatsForTopicDestination(_,_),{ stat : DestinationStatistics => stat.getMessages.getCount})
  }

  def getMessageCountForTopicDestinationCustomCounter(destinations : JMap[ActiveMQDestination,Destination],
                                         destinationName : String,
                                         counterFuction : (DestinationStatistics) => Long ) : Long = {
    getCountForDestination(destinations,destinationName,getDestinationStatsForTopicDestination(_,_),counterFuction)
  }


  def getProducerCountForTopicDestination(destinations : JMap[ActiveMQDestination,Destination],
                                          destinationName : String) : Long = {

    getCountForDestination(destinations,destinationName,getDestinationStatsForTopicDestination(_,_),{ stat : DestinationStatistics => stat.getProducers.getCount})
  }

}