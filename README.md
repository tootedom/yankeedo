# Yankeedoo

**Yankeedoo** is a simple way to send and consume messages from ActiveMQ.

Yankeedoo can be a simple way to store a list of producers or a list of consumers scenarios that
can be used to send messages to a ActiveMQ.  One of the use cases for Yankeedoo would
be to set up a scenario that can send a number of messages to a Queue; so that you can test
a service that consumes a those messages; such as monitoring it's ability (throughput/latency)
to handle those messages.

The Yankeedoo application borrows from the *Gatling* project's way of allowing you to create
a number of scenarios, that a coded using scala.  These are compiled at runtime, and you can select
the scenrio you wish to execute.

----

## Creating a Scenario and its Container ##

A Scenario is effectively the piece of work you want to perform, it could be any of the following:

- Sending messages to a topic or a queue.
- Consuming message from a queue

These Scenarios are then held within a ScenarioContainer which is executed.  A Scenario contains one MQ action, i.e.
publishing or consuming.  The Container is use to start the scenarios in order.  The Scenarios are executed inside of
an Akka Actor utilising Apache Camel.


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

## DSL ##

A simple DSL exists to ease the creation of `org.greencheek.jms.yankeedo.structure.scenario.Scenario` objects.
The DSL is imported with the following line, and what follows provide examples on how the DSL can be
used to create various scenarios:

    import org.greencheek.jms.yankeedo.structure.dsl.Dsl._


### Produce to a Queue 100 Messages ###

The following creates a producer that sends 100 message to the queue *YankeedooProductAndConsumeToQueueExample*

    createScenario(
        "Product 100 messages scenario" connect_to "tcp://localhost:61616?daemon=true&jms.closeTimeout=200"
        until_no_of_messages_sent 100
        produce to queue "YankeedooProductAndConsumeToQueueExample"
    )

By default the messages will be sent with persistent delivery, and the send will be asynchronous

### Produce to a Queue for a duration ###

The following sends to a queue for 3 seconds, as many messages as possible

    createScenario(
        "produce messages for 3 seconds scenario, with delay" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
        run_for Duration(3,SECONDS)
        until_no_of_messages_sent -1
        produce to queue "delayedqueue"
    )

The following sends to a queue for 3 seconds, as many messages as possible; but each message is sent
with a duration of 1 second between each message send

    createScenario(
        "produce messages for 3 seconds scenario, with delay" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
        run_for Duration(3,SECONDS)
        until_no_of_messages_sent -1
        produce to queue "delayedqueue"
        with_per_message_delay_of Duration(1,SECONDS)
    )

The following sends to a queue with no persistence

    createScenario(
        "produce messages for 3 seconds scenario, with delay" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
        run_for Duration(3,SECONDS)
        until_no_of_messages_sent -1
        produce to queue "delayedqueue"
        with_no_persistent_delivery and
        with_per_message_delay_of Duration(1,SECONDS)
    )

The following sends to a queue with persistent messages, but sends the message asynchronously

    createScenario(
        "produce messages for 3 seconds scenario, with delay" connect_to "tcp://localhost:" +  port + "?daemon=true&jms.closeTimeout=200"
        run_for Duration(3,SECONDS)
        until_no_of_messages_sent -1
        produce to queue "delayedqueue"
        with_persistent_delivery and
        with_no_broker_ack and
        with_per_message_delay_of Duration(1,SECONDS)
    )

Please see the following for more information about persistent messaging and asynchronous sends
http://activemq.apache.org/how-do-i-enable-asynchronous-sending.html and http://activemq.apache.org/what-is-the-difference-between-persistent-and-non-persistent-delivery.html