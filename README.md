# Overview
This project is broken down into a number of steps, each step demonstrates a concept of akka reactive streams.  Each step is in its own package with a unit test to demonstrate the stream(s) in action.

The initial examples were taken from the akka streams [Quick Start Guide](http://doc.akka.io/docs/akka/2.5.3/scala/stream/stream-quickstart.html).  But they are wrapped in a project that builds with sbt with out missing dependencies, have all imports needed to compile and run and have been extensively (perhaps excessively) commented. 

Each step has a unit test to show compliance.  The accompanying project uses `org.scalatest.FunSpec`.

# Steps
## Step 1 -- basic stream
1. Create an akka reactive stream: that has a source that provides a sequence of integers from 1 to a maximum.
1. Wrap this stream in a class for was of testing
1. Provide a method on the class that will run the stream and execute a function on each element.  The class method has the following signature: 
   ```scala
     def executeWith(f: (Any) => Unit)(implicit materializer: Materializer):Future[Done]
   ```
1. Create a Unit (spec) test that instantiates the class that wraps the stream and calls the `executeWith` method.  Use a simple function that prints the contents of the stream to the console

## Step 2 -- Add some processing during stream execution 
1. Augment the class created in step 1 with a method that runs the stream, generates factorials of each element and then executes a function on each calculated factorial.  The class method has the following signature:
   ```scala
     def executeFactorialsWith(f: (Any) => Unit)(implicit materializer: Materializer): Future[Done]
   ```
1. Create a Unit (spec) test that instantiates the class that wraps the stream and calls the `executeFactorialsWith` method.  Use the same function definition from step 1.

## Step 3 -- Create a resuseable flow and sink
1. Augment the class created in step 2 with:
   1. convert each factorial from a number to a string with the number and the resulting factorial
   2. write each number/factorial combination to a file
   3. create a reusable *Sink* that takes `Any` and writes the `Any` to a file, appended with a new line
1. Create a Unit (spec) test to demonstrate this functionality.  

## Step 4 -- Demonstrate Flow Control
This step will demonstrate that not all sources emit at the same rate, that is some sources emit data slower than other sources.
1. Augment the class created in step 3 with:
   1. Slow down the factorial source such that it only emits factorials at a rate of 1 per 100 milliseconds (fake that the calcualtion is a long running process)
   1. Demonstrate this by printing the delay inserted by each calculation in the output
   1. Don't use Thread.sleep (although you could...)
1. Create a Unit (spec) test to demonstrate this functionality.  

## Step 5 -- Use an actor to generate source values
This step will replace the simple `Iterator[Int]` that was the source of the stream data with an actor that reactively pushes data into the stream based on stream consumption
1. Augment the class created in step 3 with:
   1. Replacing the 
         ```scala
         val source: Source[Int, NotUsed] = Source(1 to max)
         ```
      with an actor based source.
   1. Create and actor that inserts data into the stream only when the stream is ready, that is it has consumed the last element inserted.
1. Create a Unit (spec) test to demonstrate this functionality.  

## Step 6
Rather than a simple stream of integers, build a stream of objects.  
1. Use the following as the basis for the object
      ```scala
      case class Comment(postId: Int, id: Int, name: String, email: String, body: String)
      ```
1.  replace the ```SourceActor``` from step 5 with a ```SourceActor``` that uses a fixed sized internal list of ```Comment``` objects rather than a variable length iterator of integers.
1.  replace the ```IntegerEmitter``` from step 5 with an ```CommentEmitter```
   1. change ```executeWith(f: (Any) => Unit)... ``` to ```exectueWith(f: (Comment) => Unit)....`.
   1. remove ```executeFatorialsWith```.
   1. replace ```executeFactorialsWith(fileName: String)...``` to ```executeToFile(fileName: String)...``` where the ```Comment``` objects are written as and array of json objects in the format:
      ```json
         {
            "postId": 1,
            "id": 1,
            "name": "id labore ex et quam laborum",
            "email": "Eliseo@gardner.biz",
            "body": "laudantium enim quasi est quidem magnam voluptate ipsam eos\ntempora quo necessitatibus\ndolor quam autem quasi\nreiciendis et nam sapiente accusantium"
         }
         ```
1. Create a Unit (spec) test to demonstrate this functionality.  

## Step 7
Rather than a fixed list of comments, get the comments from a web service.  This adds some delay in putting data into the stream (queue) such that the down stream processing will have to wait for the web service to provide the data to process.  (This will change as the down stream processing starts blocking and/or taking longer to process than simply writing to the console or a file)

In order to separate concerns and isoloate the web interface, create a ```JsonWebDataSource``` actor that is responsible for gathering data from the web and have the ```SourceActor``` communicate with this JsonWebDataSource actor.  That way we have one set of messages between the ```SourceActor``` and the ```Queue``` and a different set between the ```JsonWebDataSource``` and the web.  That way we can clearly see how we would change to a different web source or switch completely to a Queue (such as RabbitMQ), a postgres database query, or an amazon service such as DynamoDB.

1. Create a ```JsonWebDataSource``` actor to read from the web [JsonPlaceHolder](https://jsonplaceholder.typicode.com/comments)
   - Since the web interface returns either a single comment (based on id) or all comments (500) have this class keep track which comments (based on id) have been retrieved and only return the 'next' comment.
1. Augment the ```SourceActor``` from step 6 to get the next comment from the ```JsonWebDataSource``` actor until some maximum number of comments have been retrieved.  (basically replace the ```pipe(queue.offer(...))``` with a tell to the ```JsonWebDataSource``` actor and then respond to the result from the ```JsonWebDataSource``` with the ```pipe(queue.offer(...))```
1. Create a wrapper class ```CommentEmitter7```
1. Create a Unit (spec) test to demonstrate this functionality.  

## Step 8
So far the SourceActor has been placing data in the queue as the downstream processed comments. This means that the downstream had to wait for the SourceActor (and the call to the web service) before it could process the next comment.  

This step will initially load the queue to its maximum size and then fetch the next comment as the downstream finishes each comment so that the down stream can process pre-loaded comments while the SourceActor is getting the next data to process.  This should keep the queue full and keep the downstream from waiting on retrieving messages from the web service.

1. Augment the ```SourceActor``` in step 7 to pre-fectch/pre-load the queue with data (upto the maximum size of the queue) and then insert data into the queue as data is procesed.
1. In the Unit(Spec) test, demonstrate differences in speed when pre-fetching data.

## Step 9 Working with flows
In previous steps we have 2 methods on our ```CommentEmitter```, one that executes a function on each element and one that prints the json representation of the elements to a file.

In this step we want to combine both methods into a single *RunnableGraph*  (a Runnable graph is a source -> sink representation).  We will start in this step by executing each method in sequence as a *Flow*.  Since the operations in the flow (namely writing to the console and writing to a file) are potentially blocking operations, include flows for blocking (not wrapped in a future) and flows that do not block (wrap the call in future)

1. Add a new method to the ```CommentEmitter``` in step 8 that will print the json representation of the element to the console _and then_ writes the json representation to a file.  Represent each step as a *Flow* and connect the source to the first flow, then the second flow, and then to a sink (that actually does nothing)
1. Refactor the ```SourceActor``` from step 8 to the parent package so that it can be reused in subsequent steps.
1. In the Unit(Spec) test, demonstrate this functionality
1. Also in the Unit(Spec) test demonstrate the speed enhancements of wrapping the blocking methods in futures.

## Step 10 Parallelism (Fan-out and Fan-in)
Based on the work done in step 9, we have two flows that could run in parallel, the ```flowWith``` and the ```flowToFile``` flows.

In this step we will execute these flows in parallel and combine the results.  This will be accomplished by using *RunnableGraph*s and the *Fan-out* and *Fan-in* junctions.

1. Add a new method to the ```CommentEmitter``` in step 9 that will print the json representation of the element to the console _while at the same time_ write the json representation of the comment to a file.  
1. Write a Unit(Spec) test that demonstrates this parallel execution.
1. Also in the Unit(Spec) test demonstrate the speed enhancements of running the flows in parallel.

## Step 11 Error Handling

Now that we have flows that are somewhat self-contained, that is they do what they need to do and report success/failure, let's do something with those success/failure.

1. Add a new flow that reports on the success/failure of the steps in the flow.
1. Write a Unit(Spec) test that demonstrates this parallel execution.

## Step 12 Aysnc vs. Future
Akka uses a technique called [Operator Fusion](http://doc.akka.io/docs/akka/snapshot/scala/stream/stream-flows-and-basics.html#operator-fusion).  In a nutshell, this means that all flows are executed using the same actor.

In this step we will investigate the options for parallel processing:
- futures
- async

To do this we will create 3 different flows that:
- take the function: ```function Comment => FlowStatus``` and process each flow on the same actor
- take the function ```function Comment => Future[FlowStatus]``` and process each flow on the same actor
- take a simple ```function Comment => FlowStatus``` but use a different actor for each flow

And we create 3 different flows that take a fileName as an argument but:
- write directly to the file (blocking), using the same actor
- write directly to the file (blocking) using different actors
- write directly to the file in a future (non-blocking) using the same actor
- write directly to the file in a future (non-blocking) using different same actors

1. Add these flows.
1. Write a Unit(Spec) test to demonstrate

Note: because of the variance in web call times, the results of the different tests are inconclusive.  This is still valuable to demonstrate the different flow techniques

## Step 13 Dynamic Branch
So far all comments have followed the same branch.  In this step we will implement a dynamic branching strategy that uses a list of 'special' quotes through a different process than the rest of the comments.

The flow should look like:
```text

 the graph generally looks like: (replacing the flows with specific flow types)
   the lower flow (flowC) is chosen when the postId is in the 'special' list
   otherwise the top (FlowA/FlowB/flowD) is chosen

                                                               / -- flowA --\
                                             /-- toProcess -->               | -- flowD --\
                                            /                  \ -- flowB --/              \
                                           /                                                \
                                          /     (non-specials)                               \
   source --> flowReport --> byPostId -->    --------------------------------------------     |--> sink
                                          \     (specials)                                   /
                                           \                                                /
                                            \--------------------- flowC -----------------/

```

To implement this we need a  broadcast the is able to branch based on some criteria.





## Step 14
The last several steps have been focusing on processing, that is defining the source to sink data flow.  We have delegated the specific processing at each step (flow) to a function with one of the following signatures:
- ```scala Comment => FlowStatus```
- ```scala Comment => Future[FlowStatus]```
and then allowed for some mixing and matching of flows into different graphs (see steps 10 through 13)

Now we are going to concentrate on the different types of sources.
### Source Patterns
-  Single pass (one time through)
    - fixed size, small memory (ie. single batch)
    - fixed size, large memory  (ie. sub-batches)
- Multiple pass (repeat until done)
    - fixed size, small memory (ie. single batch)
    - fixed size, large memory  (ie. sub-batches)
- Dynamic size (data is added independent of processing)

#### Single Pass
If the source data source is small, say a list of integers and known when the stream is run, then storing them in a simple list is fine.  For example running a query against a database to find the data that needs to be processed, storing that result set as a List in the ```SourceActor```, and then have the ```SourceActor``` emit elements to the queue as needed by the stream.

If the source data is too big to fit in memory, but still fixed a couple of options are available:
1. _onDemand_ --have the ```SourceActor``` retrieve the data as required by the stream.  This is what the current ```SourceActor``` is doing by calling the web service for the next comment in response to the ```Enqueued``` messsage from the queue.  The disadvantage is that you have many individual requests, it is rather _chatty_.
1. _batch mode_ -- have the ```SourceActor``` read data in batches and store the batch in memory.  When all of the data in the batch have been consumed, retrieve another batch.  This is similar to _paging_ on web sites. The disadvantage of this approach is that stream processing must wait while a new batch is retrieved, but there are fewer requests on the data source (db for example), it is less _chatty_

#### Multiple Pass
If the source data varies in response to the stream processing then the source can not retrieve more data until the current processing is done.  There are a couple of options here as well:
1. _just-in-time_ processing.  With this approach, the source only retrieves data when *all* of the data has been processed.  This is very similar to the _batch_ approach discussed earlier.  The ```SourceActor``` gets a batch of data to process, emits them on demand from the stream, but instead of getting the next batch when the current batch has been emitted, it waits until _all_ of the emitted elements have completed.  The disadvantage of this approach is that the entire stream has to complete (that is the queue must be empty) and then wait for the next batch to be retreived.
1. _stateful_ processing.  By adding states to the data the ```SourceActor``` can determine at query time what data to retrieve.  The states are:
   1. _ready_ -- the data has not yet started processing. This is the initial state.
   1. _inFlight_ -- the data is in the stream.  The ```SourceActor``` changes the data from _ready_ to _inFlight_ as part of the retrieving data process.
   1. _completed_ -- the data has successfully been processed by the stream.  This state is set by the last flow (or the sink) in the stream upon successful completion.  If the data needs to be processed again, then this last flow (or sink) wouild set the state to _ready_.  With this approach, the source use either _onDemand_ or _batch mode_ of the _single pass_ strategies.

#### Dynamic size
When data needs to be inserted into the stream in response to external sources, for example processing comments as they happen, then the source needs to be able to handle _back pressure_ situations when the external source produces data elements faster than the stream can consume them as well as the situation when the stream processes data elements faster than the external source can produce them.

One option here is to used an external queue (such as RabbitMQ) as the source.

Another option is to use an actor to respond to data elements from the external source by writing to a data store (db or cassandra etc.) and then message the ```SourceActor``` that there is data to consume.  The ```SourceActor``` has 2 states, one where the stream is slower than the external source and one where the external source is slower than the stream
1. _**fast external source, slow stream**_.  In this case the external source must wait for the stream.  When the message comes in that there is data available, it is ignored because the stream is not ready.  The ```SourceActor``` will read from the datastore as the stream is ready.
1. _**slow external source, fast stream**_.  In this case the stream must wait on the external source. When the message comes in that there is data available, it is handled right away because the stream is waiting.
The ```SourceActor``` toggles between these 2 states as follows:


| State    | Message | Action |
| ---------------- | :-------: | ------ |
| fast source, slow stream | data available | store the data, stream queue is full |
| fast source, slow stream | data consumed | retrieve next data from storage, if data exists then  process that data and remain in this state otherwise transition to _slow source, fast stream_ state because stream is available |
| slow source, fast stream | data available | process the data, if the stream queue is full transition to _fast source slow stream_ |
| slow source, fast stream | data consumed | retrieve next data from storage, if data exists then  process that data and remain in this state otherwise transition to fast source, slow stream_ state because stream is available |



