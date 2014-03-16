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