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
package org.greencheek.jms.yankeedo.config

import org.apache.camel.component.jms.JmsComponent
import org.springframework.context.support.{PropertySourcesPlaceholderConfigurer, ClassPathXmlApplicationContext}
import java.util.Properties
import org.greencheek.jms.yankeedo.structure.scenario.Scenario
import org.greencheek.jms.yankeedo.structure.actions.{DurableTopic, JmsConsumerAction, JmsProducerAction}

/**
 * User: dominictootell
 * Date: 06/01/2013
 * Time: 17:49
 */
class ClassPathXmlApplicationContextJmsConfiguration(val scenario : Scenario) extends JmsConfiguration {
  val context = {
    scenario.jmsAction match {
      case x:JmsConsumerAction => new ClassPathXmlApplicationContext(Array("/consumerApplicationContext.xml"), false)
      case y:JmsProducerAction => new ClassPathXmlApplicationContext(Array("/producerApplicationContext.xml"), false)
    }

  }

  setProperties(context)
  context.refresh();

  def getJmsComponent(): JmsComponent =  context.getBean("jms").asInstanceOf[JmsComponent]


  def stop() {
    context.close()
  }



  private def setProperties(context : ClassPathXmlApplicationContext) : Unit = {

    val configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setProperties(getProperties(scenario));
    configurer.setIgnoreUnresolvablePlaceholders(true);

    context.addBeanFactoryPostProcessor(configurer);
  }

  private def getProperties(scenario : Scenario) : Properties = {
    val props = new Properties();
    props.setProperty("jms.broker.url", scenario.jmsUrl);
    scenario.jmsAction match {
      case x:JmsConsumerAction => {
        x.destination match {
          case x : DurableTopic => {
            props.setProperty("jms.clientId",x.clientId)
          }
          case _ => {
            props.setProperty("jms.clientId","")
          }
        }
        props.setProperty("jms.max.concurrentConsumer",x.numberOfConsumers.toString)
        props.setProperty("jms.concurrentConsumer",x.numberOfConsumers.toString)
        props.setProperty("jms.prefetch",x.prefetch.toString)
      }
      case x:JmsProducerAction => {
        props.setProperty("jms.persistent.delivery",x.persistentDelivery.toString)
      }
    }

    props
  }
}
