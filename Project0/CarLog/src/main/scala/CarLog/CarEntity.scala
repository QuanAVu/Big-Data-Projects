package CarLog

import org.bson.types.ObjectId
import play.api.libs.json.Json

case class Account(_id: ObjectId, User: String, Pass: Int)

case class StateEntity(_id: ObjectId, California: Seq[String])

// Clarify Name and Models from a default database document and collection
case class Branding(Name: String, Models: Seq[String], Description: String, Url: String)
// Clarify Date( want correct regex format), State, City, and Branding from a default database document and collection
case class CarEntity(_id: ObjectId, LogID: Int, User: String, Date: String, City: String, State: String, Brands: Seq[Branding]){}

// Singleton for CarEntity which auto generates ObjectID where user doesn't have to enter one.
object CarEntity{
  def appl(LogID: Int, User: String, Date: String, City: String, State: String, Brands: Seq[Branding]): CarEntity = CarEntity(new ObjectId(), LogID, User, Date, City, State, Brands)
  //def apply(LogID: Int): CarEntity = CarEntity(new ObjectId(), LogID)
}

// Singleton for State
object StateEntity{
  def appl(California: Seq[String]): StateEntity = StateEntity(new ObjectId(), California)
}

object Account{
  def appl(User: String, Pass: Int): Account = Account(new ObjectId, User, Pass)
}

