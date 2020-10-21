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

  // Admin functions
  def parserLog(): Unit = {
    var continue = true

    while(continue){
      println("")
      println("#          [Managing Logs]           #")
      println("// View log (view)")
      println("// Insert log (insert filename)")
      //println("// Update log (update)")
      println("// Delete log (delete)")
      println("// Remove all logs (nuke)")
      back()
      print("-> ")
      StdIn.readLine() match {

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
                  connect.insertLog(place)
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
/*      // Added a simple update function in manage account instead
        case e if e.equalsIgnoreCase("update") => {

        }*/
          // TODO: ADD View logs function (user toList and forloop to extract single field)
        case e if e.equalsIgnoreCase("view") => {
          println("Logs Viewer:")
          connect.viewLog()

        }

        // CARE: Nuking function!
        case e if e.equalsIgnoreCase("nuke") => {
          println("ARE YOU SURE [Y/N] ?")
          print("-> ")
          StdIn.readLine() match {
            case e if e.equalsIgnoreCase("y") => {
              connect.nuke()
              continue = false
              parserLog() // Recursion
            }
            case _ => {
              continue = false
              parserLog() // Recursion
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

  // Manage account
  def manageAcc(): Unit = {
    var continue = true

    while(continue) {
      println("")
      println("#          [Managing Accounts]       #")
      println("// View account (view)")
      println("// Insert account (insert filename)")
      println("// Update account (update)")
      println("// Delete account (delete)")
      back()
      print("-> ")
      StdIn.readLine() match {
        case e if e.equalsIgnoreCase("view") => {
          println("Accounts:")
          connect.viewAcc()
        }

        case commandArgPattern(cmd, arg) if cmd.equalsIgnoreCase("insert") => {
          try{
            val openFile = Source.fromFile(arg).getLines().mkString

            val json: JsValue = Json.parse(openFile)
            //println(Json.prettyPrint(json))


            // Required formats when READING in Json data,
            // we can set values of what we want to read in
            implicit val accReads: Reads[Account] = (
                (JsPath \ "User").read[String](minLength[String](1)) and
                  (JsPath \ "Pass").read[Int]
              )(Account.appl _)


            json.validate[Account] match {
              case s: JsSuccess[Account] => {
                // If the parsing succeeded, we store the log into the database
                val place: Account = s.get

                // Check contents BEFORE inserting into Mongo
                val u = place.User

                if(connect.checkAcc(u) == 0){
                  connect.insertAcc(place)
                }
                else{
                  println("ERROR: User name already taken!")
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
          var continue1 = true
          println("Enter User and Password of the account you want to delete (Enter -1 for User to quit)")
          while(continue1){
            println("User: ")
            StdIn.readLine() match {
              case "-1" => continue1 = false
              case i => {
                println("Pass: ")
                val pass = StdIn.readInt()
                if (connect.deleteAcc(i, pass) == 0) {
                  println(s"Deleted User: [$i]...")
                  continue1 = false
                  //continue = false
                }
                else {
                  println("ERROR: Account cannot be found!")
                }
              }
            }
          }
        }

        case e if e.equalsIgnoreCase("update") => {
          var continue1 = true
          println("Enter Username and Password for update (Enter -1 for User to quit)")
          while(continue1){
            println("User: ")
            StdIn.readLine() match {
              case "-1" => continue1 = false
              case i => {
                println("Current Password:")
                try{
                  val x = StdIn.readInt()
                  if(connect.user(i, x) == 0){
                    println("New Password:")
                    val a = StdIn.readInt()
                    connect.updateAcc(i, a)
                    println("PASSWORD has been changed!")
                    continue1 = false
                  }
                  else{
                    println("Cannot find the account!")
                  }
                }catch{
                  case e:Exception => println("Password can only be integers!")
                }

              }
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

  def newUser() = {
    var continue1 = true
    while(continue1){
      println("(Upload) your account JSON: ")
      println("Type (back) to go back")
      print("-> ")
      StdIn.readLine() match {
        case commandArgPattern(cmd, arg) if cmd.equalsIgnoreCase("upload") => {
          try{
            val openFile = Source.fromFile(arg).getLines().mkString

            val json: JsValue = Json.parse(openFile)
            //println(Json.prettyPrint(json))

            // Required formats when READING in Json data,
            // we can set values of what we want to read in
            implicit val accReads: Reads[Account] = (
              (JsPath \ "User").read[String](minLength[String](1)) and
                (JsPath \ "Pass").read[Int]
              )(Account.appl _)

            json.validate[Account] match {
              case s: JsSuccess[Account] => {
                // If the parsing succeeded, we store the log into the database
                val place: Account = s.get

                // Check contents BEFORE inserting into Mongo
                val u = place.User

                if(connect.checkAcc(u) == 0){
                  connect.insertAcc(place)
                  continue1 = false
                  println("Congrats! You have become a new ECS Member!")
                  println("Now you can login as a current member.")
                }
                else{
                  println("ERROR: User name already taken!")
                  println("")
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
        case e if e.equalsIgnoreCase("back") => continue1 = false
        case notRecognized => println(s"$notRecognized is not a command!\n Please enter another command...")
      }
    }
  }

  // Replace function for user log
  def replaceU(name: String) = {

  }

  def currentMem(name: String) = {
    var continue1 = true
    while(continue1){
      userMenu()
      StdIn.readLine() match {
        case e if e.equalsIgnoreCase("view") => {
          println("Your Logs: ")
          connect.viewLogMem(name)
        }

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

                // Check if log JSON username is same as the current user
                if(u == name){
                  if(connect.checkEntity(name, city, state) == 0){
                    connect.insertLog(place)
                  }
                  else{
                    println("Insertion Failed!")
                  }
                }
                else{
                  println(s"ERROR: User field [$u] in your log does not match with your Username!")
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

        case e if e.equalsIgnoreCase("replace") => {

        }

          //Delete user log
        case e if e.equalsIgnoreCase("delete") => {
          var continue2 = true
          println("Enter LogID and Date of the log you want to delete (Enter -1 for LogID to quit)")
          while(continue2){
            println("LogID: ")
            try{
              StdIn.readInt() match {
                case -1 => continue2 = false
                case i => {
                  println("Date: ")
                  val date = StdIn.readLine()
                  if (connect.deleteULog(i, name, date) == 0) {
                    println(s"Deleted log [$i]")
                    continue2 = false
                  }
                  else {
                    println("ERROR: Log cannot be found!")
                  }
                }
              }
            }catch{
              case e: Exception => println("LogID has to be an integer!")
            }

          }
        }

          // Log format
        case e if e.equalsIgnoreCase("format") => {
          println("{")
          println("  \"LogID\": integer,")
          println("  \"User\": \"your_name\",")
          println("  \"Date\": \"mm-dd-yy\",")
          println("  \"City\": \"city_name\",")
          println("  \"State\": \"state_name\",")
          println("  \"Brands\": [")
          println("     {")
          println("      \"Name\": \"brand1\",")
          println("      \"Models\": [\"model1\", \"model2\"],")
          println("      \"Description\": \"text\",")
          println("      \"Url\": \"https://www.ecsclub.com\"")
          println("     },")
          println("     {")
          println("      \"Name\": \"brand2\",")
          println("      \"Models\": [\"model1\", \"model2\"],")
          println("      \"Description\": \"text\",")
          println("      \"Url\": \"https://www.ecsclub.com\"")
          println("     }")
          println("   ]")
          println("}")
        }

        case e if e.equalsIgnoreCase("back") =>{
          continue1 = false
          println("You have successfully logged out!")
        }
        case notRecog => println(s"$notRecog is not a command!\n Please enter another command...")
      }
    }
  }

  //-------------------------------------------------------------------------------------------------------------------

  // TODO: Finish replace and delete log
  // For user: insert function can be similar to admin, delete takes user's name automatically, replace takes user's name also
  def userMenu(): Unit = {
    println("")
    println("#            [Member Menu]           #")
    println("// View log (view)")
    println("// Insert log (insert filename)")
    println("// Replace log (replace)")
    println("// Delete log (delete)")
    println("// Log Format (format)")
    println("// Type (back) to logout")
    print("-> ")
  }

  // Menu for admin mode
  def adminMenu(): Unit = {
    println("")
    println("#            [Admin Menu]            #")
    println("// Manage Logs (log)")
    println("// Manage Accounts (acc)")
    println("// Type (back) to logout of Admin Mode")
  }

  // Main menu
  def menu(): Unit = {
    println("")
    println("#               [Menu]                #")
    println("// New Member (new)")
    println("// Admin Mode (admin)")
    println("// Current Member (current)")
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
          //menu()
          continue = false
          i = 1
        }
        case e if e.matches("[A-Za-z]+") => {
          println("Password: ")
          try{
            val x = StdIn.readInt()
            // Username is found
            if (connect.user(e, x) == 0) {
              // Regular member, not for Admin
              if(e != "Admin"){
                println(s"Welcome back $e!")
                println("What would you like to do today?")
                // Calls functional menu for member
                currentMem(e)
              }

              continue = false
            }
            // Username not found
            else {
              println("Username or password is incorrect!")
            }
          }catch {
            case e:Exception => println("Password has to be integers!")
          }

        }
        case notRecognized => println(s"$notRecognized is not a valid user name!")
      }
    }
    i
  }

  // Main application prompt, contains two main modes: current members and Admin mode
  def prompt(): Unit = {

    welcome()
    //menu()

    var continue = true
    while(continue){
      menu()
      print("-> ")
      StdIn.readLine match {
          //Admin mode
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
                case e if e.equalsIgnoreCase("acc") => manageAcc()
                case e if e.equalsIgnoreCase("back") => {
                  continue1 = false
                  println("You're no longer in Admin Mode!")
                }
                case notRecognized => println(s"$notRecognized is not a command!\n Please enter another command...")
              }
            }
          }
        }
          // New user account creation
        case e if e equalsIgnoreCase("new") => {
          println("Welcome to ECS Club! Would you like to create an account? [Y/N]")
          print("-> ")
            StdIn.readLine() match {
              case e if e.equalsIgnoreCase("y") => {
                println("Great! In order to create a new account please follow the JSON format below: ")
                println("{")
                println("  \"User\": \"your_name\",")
                println("  \"Pass\": enter_digits ")
                println("}")
                println("")
                newUser()
              }
              case _ => {
                println("Sorry to hear that... :(")
                continue = false
                client.close()
              }
            }

        }
          // TODO: Complete current user functions
          // Current member mode
        case e if e equalsIgnoreCase("current") => {
          println("Please enter your Username and Password...")
          if(admin() == 0){
            // Prints out the JSON format for Logs
          }
        }

          //Quit application
        case commandArgPattern(cmd, arg) if cmd.equalsIgnoreCase("exit") =>{
          continue = false

          println("============================= GOODBYE ===================================")

          client.close()
        }
        // Always the last case
        case notRecognized => println(s"$notRecognized is not a command!\n Please enter another command...")
      }
      //quit()
    }
  }
  //client.close()
}

