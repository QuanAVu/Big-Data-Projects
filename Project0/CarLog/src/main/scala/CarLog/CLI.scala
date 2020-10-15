package CarLog

import java.io.FileNotFoundException

import org.bson.types.ObjectId
import org.mongodb.scala.MongoClient

import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map, Set}
import scala.io.{BufferedSource, Source, StdIn}
import scala.util.matching.Regex
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.collection.mutable


class CLI {

  // Don't have to keep establishing new connection to mongoDB
  val client = MongoClient()
  val connect = new Connection(client)

  val commandArgPattern: Regex = "(\\w+)\\s*(.*)".r

  def welcome(): Unit = {
    println("Welcome to CarLog! \nWould you like to see our car list?")
    println("Please enter Y for Yes or N for No (enter exit to quit): ")
  }

  def quit(): Unit = {
    println("To quit enter: exit")
  }


  def prompt(): Unit = {
    //var openedFile: BufferedSource = null

    welcome()

    var continue = true
    while(continue){

      StdIn.readLine match {

        case e if e equalsIgnoreCase("y") => println("Here is the car list: ")
        case e if e equalsIgnoreCase("n") => println("What would you like to do? ")
        case commandArgPattern(cmd, arg) if cmd.equalsIgnoreCase("file") => {
          try{
            val openFile = Source.fromFile(arg).getLines().mkString

            val json: JsValue = Json.parse(openFile)
            //println(Json.prettyPrint(json))

            // Required format when READING in Json data,
            // we can set values of what we want to read in
            implicit val brandsReads: Reads[Branding] = (
              (JsPath \ "Name").read[String](minLength[String](1)) and
                (JsPath \ "Numbers").read[Int](min(0) keepAnd max(90))and
                (JsPath \ "Models").read[Seq[String]]
            )(Branding.apply _)


            implicit val entityReads: Reads[CarEntity] = (
              (JsPath \ "LogID").read[Int](min(1) keepAnd max(100)) and
                (JsPath \ "Date").read[String](minLength[String](8)) and
                (JsPath \ "City").read[String](minLength[String](1)) and
                (JsPath \ "State").read[String](minLength[String](2)) and
                  (JsPath \ "Brands").read[Seq[Branding]]
              )(CarEntity.appl _)


            json.validate[CarEntity] match {
              case s: JsSuccess[CarEntity] => {
                // If the parsing succeeded, we store the log into the database
                val place: CarEntity = s.get
                connect.insert(place)

              }
              case e: JsError => {
                // error handling flow
                println(s"Parsing Failed: $e")
              }
            }

          }catch {
            case fnf: FileNotFoundException => println(s"Failed to find file $arg")
          }
          quit()
        }
        case commandArgPattern(cmd, arg) if cmd.equalsIgnoreCase("exit") => continue = false
        // Always the last case
        case notRecognized => println(s"$notRecognized is not a command!\n Please enter another command...")
      }
      //quit()
    }
  }
}

