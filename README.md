## Creating a Scenario and its Container ##

A Scenario is effectively the piece of work you want to perform, it could be any of the following:

- Sending messages to a topic or a queue.  
- Consuming message from a queue

These Scenarios are then held within a ScenarioContainer which is executed.  A Scenario contains one MQ action, i.e. 
publishing or consuming.  The Container is use to start the scenarios in order.  The Scenarios are executed inside of
an Akka Actor utilising Apache Camel.

----

# Shout outs/Thanks to #

Major Major thanks to the Gatling application (*https://github.com/excilys/gatling*).   A 
proportion of this application would not have been possible without the Gatling application.
That of the Compilation of on the fly Scala source.  The gatling load testing tool is extremely
flexible as it allows you to write your load test scenarios as scala files; which it compiles at start up.

This allows you to write any scala code to run your load test, giving the user a huge amount
of flexibility.

The Yankeedoo application has taken the idea behind this, and also adapted the code it used to compile
it's load testing scenarios; in order to compile the AMQ Scenarios below.  Therefore massive amount of
credit goes to the Gatling project for working out how to you the Zinc compiler programatically 

----
    
# Usage #

You can use the library in two ways:

- Download the distribution, drop scala scenarios in *<DISTRO_DIR>/user-files/*, cd to *<DISTRO_DIR>* and execute *./bin/yankeedoo.sh*
- Include the library (via a dependency, i.e. maven), and use within your test code.

The distribution can be found in either *.tar.gz* or *.zip*:

- http://search.maven.org/remotecontent?filepath=org/greencheek/mq/yankeedo-distro/0.1.4/yankeedo-distro-0.1.4-bundle.tar.gz
- http://search.maven.org/remotecontent?filepath=org/greencheek/mq/yankeedo-distro/0.1.4/yankeedo-distro-0.1.4-bundle.zip

The maven dependency is on maven central and can be included with the following:

    <dependency>
        <groupId>org.greencheek.mq</groupId>
        <artifactId>yankeedo-core</artifactId>
        <version>0.1.4</version>
        <scope>test</scope>
    </dependency>
    
If you want to use the extra message sources (discussed later), can be included with the following:

    <dependency>
        <groupId>org.greencheek.mq</groupId>
        <artifactId>yankeedo-messagesources</artifactId>
        <version>0.1.4</version>
        <scope>test</scope>
    </dependency>
    
    
## Distribution Quick Example ##

Here is an example that you can drop inside the *user-files* directory (this example comes with the distribution);
and then run the *./bin/yankeedoo.sh* file.


    import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer
    import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
    import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.SystemOutToStringCamelMessageProcessor

    class ProduceAndConsumeToQueueExample extends ScenarioContainer {
      withScenarios(
        List(
          createScenario(
            "Consumer messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
              until_no_of_messages_sent 100
              consume from queue "YankeedooProductAndConsumeToQueueExample"
              prefetch 10
              with_message_consumer SystemOutToStringCamelMessageProcessor
          ),
          createScenario(
            "Product 100 messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
              until_no_of_messages_sent 100
              produce to queue "YankeedooProductAndConsumeToQueueExample"
              with_persistent_delivery
          )
        )
      )
    } 
    
## Maven Quick Example ##

Here is an example Scala App that sends a default message to a queue:


    import akka.camel.CamelMessage
    import org.greencheek.jms.yankeedo.app.ScenarioContainerExecutor
    import org.greencheek.jms.yankeedo.scenarioexecution.producer.message.CamelMessageSource
    import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
    import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer
    import scala.concurrent.duration._

    object MainApp extends App {

      val messageSource = new CamelMessageSource {
        def getMessage: CamelMessage = CamelMessage("Hello World",Map("time" -> System.currentTimeMillis()))
      }

      val producerScenario1 = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
          run_for Duration(3,SECONDS)
          until_no_of_messages_sent -1
          produce to queue "scenariocontainer"
          with_message_source messageSource
          with_persistent_delivery and
          with_per_message_delay_of Duration(1,SECONDS)    
      )

      val scenarioContainer = ScenarioContainer(producerScenario1)

      ScenarioContainerExecutor.executeScenarios(scenarioContainer,Duration(5,SECONDS))
    }

----     

# DSL #

A simple DSL exists to ease the creation of `org.greencheek.jms.yankeedo.structure.scenario.Scenario` objects.
The DSL is imported with the following line, and what follows provide examples on how the DSL can be 
used to create various scenarios:

    import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
    
## Producers ##

The following shows the various options that are available in the DSL for configuring
a producer, that will send messages to a AMQ.
    
    
### Produce to a Queue 100 Messages ###

The following creates a producer that sends 100 message to the queue *YankeedooProductAndConsumeToQueueExample*

    createScenario(
        "Product 100 messages scenario" 
        connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
        until_no_of_messages_sent 100
        produce to queue "YankeedooProductAndConsumeToQueueExample"
    )   
    
By default the messages will be sent with persistent delivery, and the send will be asynchronous

### Produce to a Queue for a duration ###

The following sends to a queue for 3 seconds, as many messages as possible

    createScenario(
        "produce messages for 3 seconds scenario, with delay" 
        connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
        run_for Duration(3,SECONDS)
        until_no_of_messages_sent -1
        produce to queue "delayedqueue"
    )
        
### Produce to a Queue, Sending message with a delay ###
    
The following sends to a queue for 3 seconds, as many messages as possible; but each message is sent 
with a duration of 1 second between each message send    
    
    createScenario(
        "produce messages for 3 seconds scenario, with delay" 
        connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
        run_for Duration(3,SECONDS)
        until_no_of_messages_sent -1
        produce to queue "delayedqueue"
        with_per_message_delay_of Duration(1,SECONDS)
    )
    
### Produce to a Queue, but Specify the message need not be persisted ###    
    
The following sends to a queue with no persistence

    createScenario(
        "produce messages for 3 seconds scenario, with delay" 
        connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
        run_for Duration(3,SECONDS)
        until_no_of_messages_sent -1
        produce to queue "delayedqueue"
        with_no_persistent_delivery and
        with_per_message_delay_of Duration(1,SECONDS)
    )
    
### Produce to a queue, but make the send asynchronous (i.e don't wait for broker ack ###    

The following sends to a queue with persistent messages, but sends the message asynchronously

    createScenario(
        "produce messages for 3 seconds scenario, with delay" 
        connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
        run_for Duration(3,SECONDS)
        until_no_of_messages_sent -1
        produce to queue "delayedqueue"
        with_persistent_delivery and
        with_no_broker_ack and
        with_per_message_delay_of Duration(1,SECONDS)
    )

Please see the following for more information about persistent messaging and asynchronous sends:
- http://activemq.apache.org/how-do-i-enable-asynchronous-sending.html and 
- http://activemq.apache.org/what-is-the-difference-between-persistent-and-non-persistent-delivery.html  

----

### Configuring the message sent ###

In the above, there was no actual mention of the contents of the message being sent or what
was sent.  By default message sent to the MQ is of the form:

    new CamelMessageSource {
        def getMessage: CamelMessage = CamelMessage(UUID.randomUUID(),Map.empty)
    }
    

The meaning of the above being: 

- Send a message whose body is a java generated UUID, which no jms headers.

The *CamelMessageSource* is a trait, which has one method that returns a CamelMessage

    trait CamelMessageSource {
        def getMessage : CamelMessage
    }

It is completely possible to send any message body, and any range of headers.  The *yankeedoo-messagesources* library:

    <dependency>
        <groupId>org.greencheek.mq</groupId>
        <artifactId>yankeedo-messagesources</artifactId>
        <version>0.1.3</version>
        <scope>test</scope>
    </dependency>
    
Contains a couple of helpers that can:

- Read the contents of a file, using that as the message content
- Round Robin read the contents of a set of files in a directory, using those as message content

Within the distribution an example exists that reads a file (ProduceAndConsumeToQueueFromFileExample) 
from the *<DISTRO_DIR>/data-files* directory:

    import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer
    import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
    import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.SystemOutToStringCamelMessageProcessor
    import org.greencheek.jms.yankeedo.scenarioexecution.producer.message.FileBasedMessageSource
    import scala.reflect.io.File

    class ProduceAndConsumeToQueueFromFileExample extends ScenarioContainer {
      val testDirectoryPath =  this.getClass.getResource("/ProduceAndConsumeToQueueFromFileExample").getPath

      withScenarios(
        List(
          createScenario(
            "Consumer messages scenario only consume half" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
              until_no_of_messages_sent 50
              consume from queue "YankeedooProductAndConsumeToQueueExampleViaFile"
              prefetch 10
              with_message_consumer SystemOutToStringCamelMessageProcessor
          ),
          createScenario(
            "Product 100 messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
              until_no_of_messages_sent 100
              produce to queue "YankeedooProductAndConsumeToQueueExampleViaFile"
              with_message_source new FileBasedMessageSource(testDirectoryPath,false,messageHeaders)
              with_persistent_delivery
          )
        )
      )

      def messageHeaders = Some((file : Option[File]) => {
          file match {
            case Some(f) => {
              Map( "FileName" -> f.name, "Last-Modified" -> f.lastModified,
                "SystemCurrentTimeInMillis" -> System.currentTimeMillis())
            }
            case None => {
              Map("SystemCurrentTimeInMillis" -> System.currentTimeMillis())
            }
          }
        })
    }
    
    
The following example isn't part of the distribution, however, imagine you have the following files, within the *sample-files*
directory within the *data-files* directory, as follows:    
    
    |- data-files
    |__ sample-files
        |- file1.json
        |- file1.xml
        |- file2.json    


Putting the following code within the *<DISTRO_DIR>/user-files/scenarios/* directory:

    import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer
    import org.greencheek.jms.yankeedo.structure.dsl.Dsl._
    import org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor.SystemOutToStringCamelMessageProcessor
    import org.greencheek.jms.yankeedo.scenarioexecution.producer.message.{DirectoryBasedMessageSource, FileBasedMessageSource}
    import scala.reflect.io.File

    /**
     * Created by dominictootell on 16/03/2014.
     */
    class ProductAndConsumeToQueueFromADirectory extends ScenarioContainer {
      val testDirectoryPath =  this.getClass.getResource("/sample-files").getPath

      val directorySource = new DirectoryBasedMessageSource(path = testDirectoryPath,
        filter = DirectoryBasedMessageSource.FILTER_BY_EXTENSION("json"),
        sortOrder = DirectoryBasedMessageSource.SORT_BY_NAME_CASE_SENSITIVE,
        sendFilesAsBytes = false, messageHeaders = Some( (file:File) => {
          Map( "FileName" -> file.name, "LastModified" -> file.lastModified)
        }))

      withScenarios(
        List(
          createScenario(
            "Consumer messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
              until_no_of_messages_sent 4
              consume from queue "YankeedooProductAndConsumeToQueueFromDirectoryExample"
              prefetch 10
              with_message_consumer SystemOutToStringCamelMessageProcessor
          ),
          createScenario(
            "Product 100 messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
              until_no_of_messages_sent 10
              produce to queue "YankeedooProductAndConsumeToQueueFromDirectoryExample"
              with_message_source directorySource
              with_persistent_delivery
          )
        )
      )

    }
     
----

## Consumers ##

The following shows the various options that are available in the DSL for configuring
a consumer, that will read message from AMQ:


### Consume a specific number of messages ###

The following consumes 5 message from the broker.  It creates 1 consumer that is taking messages
from the queue *consumerqueue*.  Only one message is prefetched by this consumer at a time.  

    createScenario(
        "consumer 10 messages" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
        until_no_of_messages_consumed 5
        consume from queue "consumerqueue"
        prefetch 1
    )    
    
### Create many consumers on a queue ###        

The following consumes 10 messages from the broker on the queue *consumerqueue*, but creates 10 consumers for that
queue.

    createScenario(
        "consumer 10 messages" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
        until_no_of_messages_consumed 5
        consume from queue "consumerqueue"
        prefetch 1
        number_of_consumers 10
    )    
    
### A Custom Message Processor ###

By default when you create a consumer, the *CamelMessage (http://doc.akka.io/api/akka/2.3.0/index.html#akka.camel.CamelMessage)*
will just be printed to Stdout, via a normal println(message).  This is perform using a default *CamelMessageProcessor* (SystemOutToStringCamelMessageProcessor).     

    package org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor

    import akka.camel.CamelMessage

    /**
     * User: dominictootell
     * Date: 06/01/2013
     * Time: 17:38
     */
    object SystemOutToStringCamelMessageProcessor extends CamelMessageProcessor{
      def process(message: CamelMessage) {
        println(message)
      }

      def consumerOnError: Boolean = true
    }

When creating a consumer you can specify a custom message process by implementing the following simple trait,
and specifying your implementation within the createScenario.

    package org.greencheek.jms.yankeedo.scenarioexecution.consumer.messageprocessor

    import akka.camel.CamelMessage

    trait CamelMessageProcessor {
      def process(message : CamelMessage)
      def consumerOnError : Boolean
    }

For example here is a message process that simply counts the number of messages it has consumed.
The below creates a Consumer that runs until it has consumed 10 messages, using a custom message processor 
to handle the incoming consumer

    val messageProcessor = new CountingMessageProcessor()

    val consumerScenario1 = createScenario(
        "produce 10 message scenario" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
        until_no_of_messages_consumed 10
        consume from queue "scenariocontainer"
        with_message_consumer messageProcessor
        prefetch 1
    )
    
    class CountingMessageProcessor extends CamelMessageProcessor {
        @volatile var _numberOfMessagesProcessed : Int = 0

        def process(message: CamelMessage) {
            _numberOfMessagesProcessed+=1
        }

        def consumerOnError: Boolean = true

        def numberOfMessagesProcessed : Int = {
            _numberOfMessagesProcessed
        }
    }



----

### Statistics ###



    ================================================================================
    Consumer messages scenario
    ================================================================================
    number of messages:                               100.00
    min value:                                          2.00 ms
    max value:                                        150.99 ms
    mean:                                              82.28 ms (12.15 msg/sec)
    stddev:                                            57.75 ms (17.32 msg/sec)
    80.00%ile:                                        148.90 ms (6.72 msg/sec)
    90.00%ile:                                        149.95 ms (6.67 msg/sec)
    99.00%ile:                                        150.99 ms (6.62 msg/sec)
    99.90%ile:                                        150.99 ms (6.62 msg/sec)

    ================================================================================

    ================================================================================
    Product 100 messages scenario
    ================================================================================
    number of messages:                               100.00
    min value:                                          0.00 ms
    max value:                                        152.04 ms
    mean:                                              82.05 ms (12.19 msg/sec)
    stddev:                                            59.07 ms (16.93 msg/sec)
    80.00%ile:                                        150.99 ms (6.62 msg/sec)
    90.00%ile:                                        150.99 ms (6.62 msg/sec)
    99.00%ile:                                        150.99 ms (6.62 msg/sec)
    99.90%ile:                                        152.04 ms (6.58 msg/sec)

    ================================================================================

### Distribution ###

The distribution can be found in either *.tar.gz* or *.zip*:

- http://search.maven.org/remotecontent?filepath=org/greencheek/mq/yankeedo-distro/0.1.4/yankeedo-distro-0.1.4-bundle.tar.gz
- http://search.maven.org/remotecontent?filepath=org/greencheek/mq/yankeedo-distro/0.1.4/yankeedo-distro-0.1.4-bundle.zip


The distribution folder structure looks as follows:

    |-bin
    |-conf
    |-data-files
    |-lib
    |-results
    |-scenario-lib
    |-user-files
    |---scenarios
    |-----advanced
    |-----basic
    


    
    
