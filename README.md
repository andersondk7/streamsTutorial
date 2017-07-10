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

## Step 13 Dynamic Input
So far there has been an inexhaustable supply of comments (well, a list of 500 anyway) and the rate of comment processing has been determined by how fast a comment can be processed.

In this step rather than already having all comments, we will simulate the generation of comments and process comments as they are generated.  The general flow is:
1. As a comment is generated it is stored in a database and the sourceActor is notified.
1. If there is room in the stream's queue (that is processing comments is faster than generating comments), the comment is read from the database and added to the queue and processed.
1. If there is not room in the stream's queue (that is generating comments is faster than processing comments) then the comment is ignored (meaning that it stays in the database for later processing).
1. Once there is space in the queue, comments are retrieved from the database,



