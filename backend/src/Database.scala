import java.sql.{Connection, DriverManager, ResultSet}

object Database {
  val url = "jdbc:mysql://localhost/mysql?serverTimezone=UTC"
  val username = "root"
  val password = "12345678"
  var connection: Connection = DriverManager.getConnection(url, username, password)

  def setupTable(): Unit = {
    val statement = connection.createStatement()

    statement.execute("DROP TABLE IF EXISTS tas")
    statement.execute("CREATE TABLE tas (name TEXT, rating TEXT, class TEXT, reviews TEXT, total DOUBLE, amount INT)")
  }

  def addTA(name: String, rating: String, clas: String, reviews: Array[AnyRef], total: Double, amount: Int): Unit = {
    val statement = connection.prepareStatement("INSERT INTO tas VALUE (?, ?, ?, ?, ?)")

    val reviewsArr = connection.createArrayOf(String, reviews)
    statement.setString(1, name)
    statement.setString(2, rating)
    statement.setString(3, clas)
    statement.setArray(4, reviewsArr)
    statement.setDouble(5, total)
    statement.setInt(6, amount)
    statement.execute()
  }
}
