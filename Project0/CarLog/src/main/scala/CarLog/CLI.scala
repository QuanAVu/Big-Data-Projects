package CarLog

import java.io.FileNotFoundException

import org.bson.types.ObjectId
import org.mongodb.scala.MongoClient

import scala.io.{Source, StdIn}
import scala.util.matching.Regex
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._


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
      println("    [Managing Logs]")
      println("// View log (view)")
      println("// Insert log (insert filename)")
      println("// Update log (update)")
      println("// Delete log (delete)")
      println("// Remove all logs (nuke)")
      back()
      print("-> ")
      StdIn.readLine() match {
          // TODO: Complete contents checking
        case commandArgPattern(cmd, arg) if cmd.equalsIgnoreCase("insert") => {
          try{
            val openFile = Source.fromFile(arg).getLines().mkString

            val json: JsValue = Json.parse(openFile)
            //println(Json.prettyPrint(json))


            // Required formats when READING in Json data,
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

                // Check contents BEFORE inserting into Mongo
                val u = place.User
                val city = place.City
                val state = place.State
                if(connect.checkEntity(u, city, state) == 0){
                  connect.insert(place)
                }
                else{
                  println("Insertion Failed!")
                }

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

          // TODO: ADD UpdateOne function
        case e if e.equalsIgnoreCase("update") => {

        }
          // TODO: ADD View logs function (user toList and forloop to extract single field)
        case e if e.equalsIgnoreCase("view") => {
          println("Logs Viewer:")
          connect.view()

        }

        // CARE: Nuking function!
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

  // TODO: Complete User Menu, User can only insert/delete/update their own logs
  // For user: insert function can be similar to admin, delete takes user's name automatically (can perform nuking), update takes user's name also
  def userMenu(): Unit = {
    println("#            [User Menu]           #")
    println("// View log (view)")
    println("// Insert log (insert filename)")
    println("// Update log (update)")
    println("// Delete log (delete)")
    println("// Type (back) to logout")
  }

  // TODO: Add in update field function and view for log in admin, add more functions (view, insert, update, delete) to account
  def adminMenu(): Unit = {
    println("#            [Admin Menu]           #")
    println("// Manage Logs (log)")
    println("// Manage Accounts (account)")
    println("// Type (back) to logout of Admin Mode")
  }

  // Main menu
  def menu(): Unit = {
    println("#            [Menu]           #")
    println("// New User (new)")
    println("// Admin Mode (admin)")
    println("// Current User (current)")
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

                  parserLog()
                }
                case e if e.equalsIgnoreCase("account") => println("    [Managing accounts]")
                case e if e.equalsIgnoreCase("back") => { menu()
                  continue1 = false
                }
                case notRecognized => println(s"$notRecognized is not a command!\n Please enter another command...")
              }
            }
          }
        }
          // TODO: Complete new user account creation
        case e if e equalsIgnoreCase("new") => {
          println("Welcome to ECS Club! Would you like to create an account? [Y/N]")
        }
          // TODO: Complete current user functions
        case e if e equalsIgnoreCase("current") => {
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

