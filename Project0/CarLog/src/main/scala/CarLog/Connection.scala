package CarLog

import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.{MongoClient, MongoCollection, Observable}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
// Make sure to start with imports in your own code!
import org.mongodb.scala.bson.codecs.Macros._
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}


//class Connection(doc: CarEntity) {
class Connection(mongo: MongoClient){

    // Mongo scala boilerplate for localhost:
    // Include classOf[T] for all of your classes
    val codecRegistry = fromRegistries(fromProviders(classOf[CarEntity], classOf[Branding]), MongoClient.DEFAULT_CODEC_REGISTRY)
    //val client = MongoClient()
    val db = mongo.getDatabase("carlogs").withCodecRegistry(codecRegistry)
    val collection: MongoCollection[CarEntity] = db.getCollection("carbrands")
    //val collection2: MongoCollection[CarEntity] = db.getCollection("California")

    // helper functions for access and printing, to get us started + skip the Observable data type
    private def getResults[T](obs: Observable[T]): Seq[T] = {
      Await.result(obs.toFuture(), Duration(10, SECONDS))
    }

    def printResults[T](obs: Observable[T]): Unit = {
      getResults(obs).foreach(println(_))
    }

    //printResults(collection.find())

    def insert(doc: CarEntity): Unit = {
      printResults(collection.insertOne(doc))
    }

    def count() = {
        //val counter = collection.countDocuments()
        //getResults(counter).foreach(println(_))
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
