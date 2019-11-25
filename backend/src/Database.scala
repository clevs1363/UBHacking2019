import java.sql.{Connection, DriverManager, ResultSet}

import play.api.libs.json.{JsValue, Json}

import scala.collection.mutable.ListBuffer

object Database {
  val url = "jdbc:mysql://localhost:3306/mysql?serverTimezone=UTC"
  val username = "mcleversley"
  val password = "cse116rox"
  val driver = "com.mysql.cj.jdbc.Driver"
  Class.forName(driver).newInstance()
  var connection: Connection = DriverManager.getConnection(url, username, password)

  def setupTable(): Unit = {
    val statement = connection.createStatement()

    statement.execute("DROP TABLE IF EXISTS tas")
    statement.execute("CREATE TABLE tas (name TEXT, totalRating DOUBLE, amountOfRatings INT, overallRating DOUBLE, class TEXT)")
    statement.execute("DROP TABLE IF EXISTS reviews")
    statement.execute("CREATE TABLE reviews (name TEXT, rating INT, review TEXT)")
  }

  def addTA(name: String, totalRating: Double, amountOfRatings: Int, overallRating: Double, clas: String): Unit = {
    val statement = connection.prepareStatement("INSERT INTO tas VALUE (?, ?, ?, ?, ?)")

    statement.setString(1, name)
    statement.setDouble(2, totalRating)
    statement.setInt(3, amountOfRatings)
    statement.setDouble(4, overallRating)
    statement.setString(5, clas)
    statement.execute()
  }

  def addRating(taName: String, rating: Int, review: String): Unit = {
    val statement = connection.prepareStatement("INSERT INTO reviews VALUES (?, ?, ?)")

    statement.setString(1, taName)
    statement.setInt(2, rating)
    statement.setString(3, review)
    statement.execute()
  }

  def updateTA(name: String, rating: Int, review: String): String = {
    val statement = connection.prepareStatement("UPDATE tas SET totalRating = ?, amountOfRatings = ?, overallRating = ? WHERE name = ?")

    val getRow = connection.prepareStatement("SELECT * FROM tas WHERE name = ?")
    getRow.setString(1, name)
    val row = getRow.executeQuery()
    row.next()
    val currentRating = row.getDouble("totalRating") + rating
    val currentAmount = row.getInt("amountOfRatings") + 1
    val newRating = currentRating/currentAmount

    statement.setDouble(1, currentRating)
    statement.setInt(2, currentAmount)
    statement.setDouble(3, newRating)
    statement.setString(4, name)

    addRating(name, rating, review)

    statement.execute()

    newRating.toString
  }

  def getTA(name: String): String = {
    val getTAInfo = connection.prepareStatement("SELECT * FROM tas WHERE name = ?")
    getTAInfo.setString(1, name)
    val taInfo: ResultSet = getTAInfo.executeQuery()
    taInfo.next()

    val overallRating = taInfo.getDouble("overallRating")
    val clas = taInfo.getString("class")

    val getReviewsInfo = connection.prepareStatement("SELECT * FROM reviews INNER JOIN tas ON reviews.name = tas.name")

    val reviewsInfo: ResultSet = getReviewsInfo.executeQuery()
    val reviews: ListBuffer[String] = new ListBuffer[String]()
    while (reviewsInfo.next()) reviews += reviewsInfo.getString("rating") + reviewsInfo.getString("review")


    val json_name = Json.toJson(name)
    val json_overallRating = Json.toJson(overallRating)
    val json_clas = Json.toJson(clas)
    val json_reviews = Json.toJson(reviews)

    val jsonMap: Map[String, JsValue] = Map(
      "name" -> json_name,
      "overallRating" -> json_overallRating,
      "class" -> json_clas,
      "reviews" -> json_reviews
    )

    val retString = Json.stringify(Json.toJson(jsonMap))
    retString
  }
}
