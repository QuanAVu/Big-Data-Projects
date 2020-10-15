package CarLog

import scala.io.{BufferedSource, Source}

object FileUtil {
  def getTextContent(filename: String): Option[String] = {
    // The way we open files is using Source.fromFile
    // You can write short version of opening + reading from a file,
    // ours will be a little longer so we can properly close the file
    // We'll use a try finally for this

    // Both of these are just to declare outside of the try block
    var openedFile: BufferedSource = null
    //var textContent: Option[String] =
    try{
      openedFile = Source.fromFile(filename)
      // return this:
      Some(openedFile.getLines().mkString(" "))
    }finally{
      if(openedFile != null) openedFile.close()
    }
  }
}


/*{
  "Cars": {
    "Types": [
      {
        "CarID": 1,
        "Brand": "Toyota",
        "Models": [
          "Avalon",
          "Camry",
          "Corolla",
          "Highlander",
          "Prius",
          "RAV4",
          "Sienna",
          "Sequoia"
        ]
      },
      {
        "CarID": 2,
        "Brand": "Lamborghini",
        "Models": [
          "Aventador",
          "Huracan",
          "Urus"
        ]
      },
      {
        "CarID": 3,
        "Brand": "Porsche",
        "Models": [
          "718",
          "911",
          "Cayenne",
          "Macan",
          "Panamera",
          "Tacan"
        ]
      }
    ]
  }
}*/

/*
"LogID": 1,
  "Date": "10-14-20",
  "Brands": [
    {
      "Name": "Toyota",
      "Numbers": 3,
      "Models": ["Prius", "Corolla"]
    },
    {
      "Name": "Lamborghini",
      "Numbers": 1,
      "Models": ["Adventador"]
    }
  ]
 */