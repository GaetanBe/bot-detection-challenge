# Bot detection challenge
## Description

This small program uses akka stream to emulate an unbounded stream that contains connexion and performs the  
following processing :
- loop over a log file retrieved from uri "http://www.almhuette-raith.at/apache-log/access.log"
- parse every line to a record
- batch over a defined number of records
- use these batches to label a log as `authorized` or `unauthorized`
- balance the load of these predictions on a pool of workers
- stores the results in the appropriate database
- notify the client api that a record has been labeled as suspicious

It will log the operations that are currently being performed such as parsing, receiving a response from
a database or an api.

As this program rely on external calls that are here, for the sake of the exercise, random delays, we have
chosen to slow the process so that the log are readable. Then we can assess the processes previously described.

## Usage
Run the following commands to build a docker image and run it.
~~~bash
sbt docker
docker run default/bot-detection-challenge:v0.1
~~~

## Capacities and limitations
This program implements a use case of akka streams in a `at most once` semantic. As records are grouped
over an arbitrary element and asynchronous calls are being made to retrieved labels and store results,
the order of the stream is not guaranteed, and some records may be notified as totally processed to the consumer, in
an incorrect order.

See https://doc.akka.io/docs/alpakka-kafka/current/atleastonce.html#using-groupby

We consider that not accessing the correct processing of some records would not harm the system, as long 
as we are able to log them and make sure it stays irrelevant.

## Further improvements
Some notes are written in the codes' comments that show where we would add error handling, logging or monitoring.

Other than that, here are some further ideas :
- using property based test to fully cover the most important functions
- asynchronous calls
- pure IO handling using Cats or Monix libraries
