package CarLog

import org.bson.types.ObjectId
import play.api.libs.json.Json

//case class CarEntity(_id: ObjectId, LogID: Int)
case class Branding(Name: String, Numbers: Int, Models: Seq[String])

case class CarEntity(_id: ObjectId, LogID: Int, Date: String, Brands: Seq[Branding]){}

object CarEntity{
  def appl(LogID: Int, Date: String, Brands: Seq[Branding]): CarEntity = CarEntity(new ObjectId(), LogID, Date, Brands)
  //def apply(LogID: Int): CarEntity = CarEntity(new ObjectId(), LogID)
}


