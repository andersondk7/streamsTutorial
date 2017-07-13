package adc.tutorial.akka.streams.step14

import adc.tutorial.akka.streams.model.Comment
import akka.actor.{Actor, ActorLogging, Props}

import scala.util.Random

/**
  * This class 'generates' comments at random
  */
class CommentGenerator() extends Actor with ActorLogging {
  import CommentGenerator._

  val random = Random

  override def receive: Receive = onMessage(postId=1, commentCount=1, id=1)

  def onMessage(postId: Int, commentCount: Int, id: Int): Receive = {
    case NextComment =>
      val name= nextName()
      val comment = Comment(postId, id, name=name, email=nextEmail(name), body=nextBody)
      log.info(s"comment:  $comment")
      sender() ! comment
      if (commentCount == commentsPerPost) context.become(onMessage(postId+1, 1, id+1))
      else context.become(onMessage(postId, commentCount+1, id+1))
    }

}

object CommentGenerator {

  def props() = Props(classOf[CommentGenerator])
  val commentsPerPost = 5

  case object NextComment
  case class Generated(id: Int)

  private def random = Random

  def nextName(): String = names(random.nextInt(names.size))
  def nextEmail(name: String): String = s"$name@testApplication.org"
  def nextBody: String = {
    (1 to random.nextInt(20)+3).map(c => {
      val i = random.nextInt(loremWords.size)
      loremWords(i)
    }).mkString(" ")
  }

  val names = Vector(
                      "Jame", "Mar", "John", "Patricia", "Robert", "Jennifer", "Michael", "Elizabeth", "William", "Linda", "David", "Barbara",
                      "Richard", "Susan", "Joseph", "Jessica", "Thomas", "Margaret", "Charles", "Sarah", "Christopher", "Karen",
                      "Daniel", "Nancy", "Matthew", "Betty", "Anthony", "Lisa", "Donald", "Dorothy", "Mark", "Sandra",
                      "Paul", "Ashley", "Steven", "Kimberly", "Andrew", "Donna", "Kenneth", "Carol", "George", "Michelle",
                      "Joshua", "Emily", "Kevin", "Amanda", "Brian", "Helen", "Edward", "Melissa", "Ronald", "Deborah",
                      "Timothy", "Stephanie", "Jason", "Laura", "Jeffrey", "Rebecca", "Ryan", "Sharon", "Gary", "Cynthia",
                      "Jacob", "Kathleen", "Nicholas", "Amy", "Eric", "Shirley", "Stephen", "Anna", "Jonathan", "Angela",
                      "Larry", "Ruth", "Justin", "Brenda", "Scott", "Pamela", "Frank", "Nicole", "Brandon", "Katherine",
                      "Raymond", "Virginia", "Gregory", "Catherine", "Benjamin", "Christine", "Samuel", "Samantha", "Patrick", "Debra",
                      "Alexander", "Janet", "Jack", "Rachel", "Dennis", "Carolyn", "Jerry", "Emma", "Tyler", "Maria",
                      "Aaron", "Heather", "Henry", "Diane", "Douglas", "Julie", "Jose", "Joyce", "Peter", "Evelyn",
                      "Adam", "Frances", "Zachary", "Joan", "Nathan", "Christina", "Walter", "Kelly", "Harold", "Victoria",
                      "Kyle", "Lauren", "Carl", "Martha", "Arthur", "Judith", "Gerald", "Cheryl", "Roger", "Megan",
                      "Keith", "Andrea", "Jeremy", "Ann", "Terry", "Alice", "Lawrence", "Jean", "Sean", "Doris",
                      "Christian", "Jacqueline", "Albert", "Kathryn", "Joe", "Hannah", "Ethan", "Olivia", "Austin", "Gloria",
                      "Jesse", "Marie", "Willie", "Teresa", "Billy", "Sara", "Bryan", "Janice", "Bruce", "Julia",
                      "Jordan", "Grace", "Ralph", "Judy", "Roy", "Theresa", "Noah", "Rose", "Dylan", "Beverly", "Eugene", "Denise", "Wayne", "Marilyn", "Alan", "Amber", "Juan", "Madison",
                      "Louis", "Danielle", "Russell", "Brittany", "Gabriel", "Diana", "Randy", "Abigail", "Philip", "Jane",
                      "Harry", "Natalie", "Vincent", "Lori", "Bobby", "Tiffany", "Johnny", "Alexis", "Logan", "Kayla"
                     )

    val loremWords = Vector(
                            "Lorem", "ipsum", "dolor", "sit", "amet,", "consectetur", "adipiscing", "elit.", "In", "dui",
                            "ex,", "auctor", "at", "rhoncus", "vel,", "laoreet", "et", "ligula.", "Quisque", "placerat",
                            "turpis", "et", "faucibus", "sagittis.", "Praesent", "eu", "iaculis", "eros.", "Suspendisse", "tristique",
                            "orci", "non", "sem", "suscipit", "venenatis", "Nullam", "bibendum", "lorem", "in", "nisi",
                            "vulputate", "finibus.", "Integer", "sit", "amet", "enim", "eu", "nibh", "rutrum", "auctor.",
                            "Donec", "ligula", "nulla,", "porttitor", "et", "enim", "eget,", "luctus", "hendrerit", "libero.",
                            "Proin", "vel", "nibh", "vitae", "dolor", "blandit", "varius", "at", "ac", "nunc.",
                            "Proin", "accumsan,", "ex", "ac", "sagittis", "mollis,", "sem", "justo", "feugiat", "dolor,",
                            "at", "varius", "ipsum", "tellus", "eget", "ligula.", "Praesent", "sit", "amet", "massa",
                            "est.", "Sed", "ut", "sem", "fermentum,", "hendrerit", "purus", "vel,", "vestibulum", "sapien. "
                           )
}
