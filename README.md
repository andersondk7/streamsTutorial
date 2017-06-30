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
