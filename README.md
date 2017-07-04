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

In order to separate concerns and isoloate the web interface, create a ```JsonDataSource``` actor that is responsible for gathering data from the web and have the ```SourceActor``` communicate with this JsonDataSource actor.  That way we have one set of messages between the ```SourceActor``` and the ```Queue``` and a different set between the ```JsonDataSource``` and the web.  That way we can clearly see how we would change to a different web source or switch completely to a Queue (such as RabbitMQ), a postgres database query, or an amazon service such as DynamoDB.

1. Create a ```JsonDataSource``` actor to read from the web [JsonPlaceHolder](https://jsonplaceholder.typicode.com/comments)
   - Since the web interface returns either a single comment (based on id) or all comments (500) have this class keep track which comments (based on id) have been retrieved and only return the 'next' comment.
1. Augment the ```SourceActor``` from step 6 to get the next comment from the ```JsonDataSource``` actor until some maximum number of comments have been retrieved.  (basically replace the ```pipe(queue.offer(...))``` with a tell to the ```JsonDataSource``` actor and then respond to the result from the ```JsonDataSource``` with the ```pipe(queue.offer(...))```
1. Create a wrapper class ```CommentEmitter7```
1. Create a Unit (spec) test to demonstrate this functionality.  

