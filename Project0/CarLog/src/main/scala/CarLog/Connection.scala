package CarLog

import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Filters.{and, equal, exists}
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.{MongoClient, MongoCollection, Observable}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import scala.io.StdIn
// Make sure to start with imports in your own code!
import org.mongodb.scala.bson.codecs.Macros._
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}


//class Connection(doc: CarEntity) {
class Connection(mongo: MongoClient){

    // Mongo scala boilerplate for localhost:
    // Include classOf[T] for all of your classes
    val codecRegistry = fromRegistries(fromProviders(classOf[CarEntity], classOf[Branding], classOf[Account], classOf[StateEntity], classOf[Cars]), MongoClient.DEFAULT_CODEC_REGISTRY)
    //val client = MongoClient()
    val db = mongo.getDatabase("carlogs").withCodecRegistry(codecRegistry)
    val collection: MongoCollection[CarEntity] = db.getCollection("logs")
    val collection2: MongoCollection[Account] = db.getCollection("accounts")
    val collection3: MongoCollection[StateEntity] = db.getCollection("cities")
    val collection4: MongoCollection[Cars] = db.getCollection("carbrands")

    // Put main thread to sleep/run longer so that connections messages finish before prompt shows up
    Thread.sleep(1000)

    // helper functions for access and printing, to get us started + skip the Observable data type
    private def getResults[T](obs: Observable[T]): Seq[T] = {
      Await.result(obs.toFuture(), Duration(10, SECONDS))
    }

    def printResults[T](obs: Observable[T]): Unit = {
      getResults(obs).foreach(println(_))
    }

    // ------------------------------------Do Not Touch The Above Boiler----------------------------------------------
    //printResults(collection.find())

    def insertLog(doc: CarEntity): Unit = {
      printResults(collection.insertOne(doc))

    }

    def insertAcc(acc: Account) = {
      printResults(collection2.insertOne(acc))
    }

    def updateAcc(user: String, npass: Int) = {
      val check = collection2.updateOne(equal("User", user), set("Pass", npass))
      if(getResults(check).isEmpty == false){
        0
      }
      else{
        1
      }

    }

    /*
    def count() = {
        //val counter = collection.countDocuments()
        //getResults(counter).foreach(println(_))
    }*/

    // Search for the user
    def user(name: String, p: Int): Int = {
        val check = collection2.find(and(equal("User", name), equal("Pass", p)))
        //Account found, return 0
        if(getResults(check).isEmpty == false){
           0
        }
        else{
           1
        }
    }

    // Delete log
    def deleteLog(id: Int, user: String): Int = {
      val check = collection.find(and(equal("LogID", id), equal("User", user)))
        if(getResults(check).isEmpty == false){

            printResults(collection.deleteOne(and(equal("LogID", id), equal("User", user))))
            0
        }
        else{
            1
        }
    }

    def deleteAcc(user: String, pass: Int): Int = {
      val check = collection2.find(and(equal("User", user), equal("Pass", pass)))
      if(getResults(check).isEmpty == false){

        printResults(collection2.deleteOne(and(equal("User", user), equal("Pass", pass))))
        0
      }
      else{
        1
      }
    }

    def viewLog(): Any = {
      val check = collection.find()
      if(getResults(check).isEmpty == false){
        println("")
        printResults(check)

        check
      }
      else{
        println("     EMPTY   ")
      }

    }

    def viewLogMem(name: String): Any = {
      val check = collection.find(equal("User", name))
      if(getResults(check).isEmpty == false){
        println("")
        printResults(check)

        check
      }
      else{
        println("     EMPTY   ")
      }
    }

    def viewAcc(): Any = {
      val check = collection2.find()
      if(getResults(check).isEmpty == false){
        println("")
        printResults(check)
        check
      }
      else{
        println("     EMPTY   ")
      }
    }

    def checkAcc(user: String): Int = {
      val check = collection2.find(equal("User", user))
      if(getResults(check).isEmpty == false){
        //println("ERROR: User name already taken!")
        -1
      }
      else{
        0
      }
    }

    // Checks contents of incoming JSON with default documents in Mongo
    def checkEntity(user: String, city: String, state: String): Int = {
      val check1 = collection2.find(equal("User", user))

      if(getResults(check1).isEmpty == false){
        val check2 = collection3.find(equal("California", city))

        if(getResults(check2).isEmpty == false){
          val check3 = collection3.find(exists(state))

          if(getResults(check3).isEmpty == false){
            return 0
          }
          else{
            println("ERROR: State is incorrect!")
            -1
          }
        }
        else{
          println("ERROR: City is incorrect!")
          -1
        }
      }
      else{
        println("ERROR: User is incorrect!")
        -1
      }
    }

    def nuke(): Unit = {
      printResults(collection.deleteMany(exists("LogID")))
      println("=======================================")
      println("*                                     *")
      println("!!!!!!!!!! BOOOOOOOOOOOOOOOOM !!!!!!!!!")
      println("*                                     *")
      println("=======================================")
    }

    /*
    def deleteByTitle(title: String) = {
        try {
            getResults(collection.deleteMany(equal("title", title)))(0)
              .getDeletedCount > 0
        } catch {
            case e: Exception => {
                e.printStackTrace() //could be better
                false
            }
        }
    }*/

}
