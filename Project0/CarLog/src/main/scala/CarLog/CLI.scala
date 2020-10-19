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
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt, SECONDS}
import scala.concurrent.{Await, Future}


class CLI {

  val client = MongoClient()
  val connect = new Connection(client)

  val commandArgPattern: Regex = "(\\w+)\\s*(.*)".r


  def welcome(): Unit = {
    println("=========================================================================")
    println("*                                                                       *")
    println("*             Welcome to the Exotic Car Spotting (ECS) Club!            *")
    println("*                                                                       *")
    println("*  It is Fall of 2020! Despite everything else going on, I hope you're  *")
    println("*  staying safe and can find comfort in our ECS Club. Our car logging   *")
    println("*  system has never been easier to use, but feel free to submit us a    *")
    println("*  ticket if you have any questions. Have fun!                          *")
    println("*                                                                       *")
    println("=========================================================================")

  }

  // Parsing functions-------------------------------------------------------------------------------------------------
  def parserLog(): Unit = {
    var continue = true

    while(continue){
      println("// Type (insert filename) to insert log")
      println("// Type (delete) to delete log")
      println("// Type (nuke) to remove all logs")
      back()
      print("-> ")
      StdIn.readLine() match {
        case commandArgPattern(cmd, arg) if cmd.equalsIgnoreCase("insert") => {
          try{
            val openFile = Source.fromFile(arg).getLines().mkString

            val json: JsValue = Json.parse(openFile)
            //println(Json.prettyPrint(json))

            // Required format when READING in Json data,
            // we can set values of what we want to read in
            implicit val brandsReads: Reads[Branding] = (
              (JsPath \ "Name").read[String](minLength[String](1)) and
                (JsPath \ "Models").read[Seq[String]] and
                (JsPath \ "Description").read[String] and
                (JsPath \ "Url").read[String]
              )(Branding.apply _)


            implicit val entityReads: Reads[CarEntity] = (
              (JsPath \ "LogID").read[Int](min(1) keepAnd max(100)) and
                (JsPath \ "User").read[String](minLength[String](1)) and
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
        }

        case commandArgPattern(cmd, arg) if cmd.equalsIgnoreCase("delete") => {
          var continue = true
          println("Enter LogID and User of the log you want to delete (Enter -1 for LogID to quit)")
          while(continue){
            println("LogID: ")
            StdIn.readInt() match {
              case -1 => continue = false
              case i => {
                println("User: ")
                val name = StdIn.readLine()
                if (connect.deleteLog(i, name) == 0) {
                  continue = false
                }
                else {
                  println("ERROR: Log cannot be found!")
                }
              }
            }


          }
        }

        case e if e.equalsIgnoreCase("nuke") => {
          println("ARE YOU SURE [Y/N] ?")
          print("-> ")
          StdIn.readLine() match {
            case e if e.equalsIgnoreCase("y") => {
              connect.nuke()
              continue = false
              parserLog()
            }
            case _ => {
              continue = false
              parserLog()
            }

          }
        }

        case e if e.equalsIgnoreCase("back") => {
          adminMenu()
          continue = false
        }
        case notRecognized => println(s"$notRecognized is not a command!\n Please enter another command...")
      }
    }
  }
  //-------------------------------------------------------------------------------------------------------------------

  def userMenu(): Unit = {

  }

  // TODO: Add in functions for city, brand, account
  def adminMenu(): Unit = {
    println("#            [Admin Menu]           #")
    println("// Update Log (enter log)")
    println("// Update City (enter city)")
    println("// Update Brand (enter brand)")
    println("// Update Account (enter account)")
    println("// Type (back) to logout of Admin Mode")
  }

  // Main menu
  def menu(): Unit = {
    println("#            [Menu]           #")
    println("// New User (enter new)")
    println("// Admin Mode (enter admin)")
    println("// Returning User (enter return)")
    quit()
  }

  def back(): Unit ={
    println("// Type (back) to go back ")
  }

  def quit(): Unit = {
    println("// Type (exit) to quit ")
  }

  // Checking admin credentials or user's credentials
  def admin(): Int = {
    var i = 0
    var continue = true

    println("(Type back to go back to menu)")

    while(continue) {
      println("User: ")
      StdIn.readLine match {
        case e if e.equalsIgnoreCase("back") =>{
          menu()
          continue = false
          i = 1
        }
        case e if e.matches("[A-Za-z]+") => {
          println("Password: ")
          val x = StdIn.readInt()
          // Username is found
          if (connect.user(e, x) == 0) {
            if(e != "Admin"){
              println(s"Welcome back $e!")
            }

            continue = false
          }
          // Username not found
          else {
            println("Username or password is incorrect!")
          }
        }
        case notRecognized => println(s"$notRecognized is not a valid user name!")
      }
    }
    i
  }

  // Main application prompt
  def prompt(): Unit = {

    welcome()
    menu()

    var continue = true
    while(continue){
      print("-> ")
      StdIn.readLine match {
        case e if e equalsIgnoreCase("admin") =>{
          println("Please enter your Admin credentials...")
          // 0 when successfully logged in
          if(admin() == 0){
            println("You're now in Admin Mode!")
            adminMenu()
            var continue1 = true
            while(continue1){
              print("-> ")
              StdIn.readLine() match {
                case e if e.equalsIgnoreCase("log") => {
                  println("Editing Logs")
                  parserLog()
                }
                case e if e.equalsIgnoreCase("city") => println("Editing city")
                case e if e.equalsIgnoreCase("brand") => println("Editing brand")
                case e if e.equalsIgnoreCase("account") => println("Editing account")
                case e if e.equalsIgnoreCase("back") => { menu()
                  continue1 = false
                }
                case notRecognized => println(s"$notRecognized is not a command!\n Please enter another command...")
              }
            }

          }
          //continue = false
        }
        case e if e equalsIgnoreCase("new") => {
          println("new user....")
        }
        case e if e equalsIgnoreCase("return") => {
          println("Please enter your Username and Password...")
          if(admin() == 0){
            println("What would you like to do today?")
          }
        }

        case commandArgPattern(cmd, arg) if cmd.equalsIgnoreCase("exit") => continue = false
        // Always the last case
        case notRecognized => println(s"$notRecognized is not a command!\n Please enter another command...")
      }
      //quit()
    }
  }
  //client.close()
}

