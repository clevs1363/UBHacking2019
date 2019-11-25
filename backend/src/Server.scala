import java.io.File
import com.corundumstudio.socketio.listener.{ConnectListener, DataListener, DisconnectListener}
import com.corundumstudio.socketio.{AckRequest, Configuration, SocketIOClient, SocketIOServer}
import play.api.libs.json.{JsValue, Json}
import com.github.tototoshi.csv._

class Server() {

  val config: Configuration = new Configuration {
    setHostname("localhost")
    setPort(8080)
  }

  val server: SocketIOServer = new SocketIOServer(config)

  server.addConnectListener(new ConnectionListener())
  server.addDisconnectListener(new DisconnectionListener())
  server.addEventListener("get_schools", classOf[Nothing], new SchoolListener())
  server.addEventListener("getTA", classOf[String], new getTAListener())
  server.addEventListener("addTA", classOf[String], new addTAListener())
  server.addEventListener("addRating", classOf[String], new ratingListener())
  server.addEventListener("stop_server", classOf[Nothing], new StopListener(this))

  server.start()
}

object Server {
  def main(args: Array[String]): Unit = {
    Database.setupTable()
    new Server()
  }
}

class addTAListener() extends DataListener[String] {
  override def onData(socket: SocketIOClient, data: String, ackRequest: AckRequest): Unit = {
    val parsed: JsValue = Json.parse(data)
    val name: String = (parsed \ "name").as[String]
    val clas: String = (parsed \ "class").as[String]
    Database.addTA(name, 0.0, 0,  0.0, clas)
  }
}

class ratingListener() extends DataListener[String] {
  override def onData(socket: SocketIOClient, data: String, ackRequest: AckRequest): Unit = {
    val parsed: JsValue = Json.parse(data)
    println(parsed)
    val name: String = (parsed \ "name").as[String]
    val rating = (parsed \ "rating").as[String].toInt
    val review: String = (parsed \ "review").as[String]
    val newRating = Database.updateTA(name, rating, review)

    socket.sendEvent("getRating", newRating)
  }
}

class SchoolListener() extends DataListener[Nothing] {
  override def onData(socket: SocketIOClient, data: Nothing, ackRequest: AckRequest): Unit = {
    val reader = CSVReader.open(new File("../flask/static/data/collegeInfo.csv")).allWithHeaders()
    var data = ""
    for (map <- reader) {
      data += (map("INSTNM") + "$")
      data += (map("CITY") + "$")
      data += (map("STABBR") + "$")
      data += map("INSTURL")
      socket.sendEvent("school_item", data)
      data = ""
    }
  }
}

class getTAListener() extends DataListener[String] {
  override def onData(socket: SocketIOClient, name: String, ackRequest: AckRequest): Unit = {
    val data = Database.getTA(name)
    socket.sendEvent("TA", data)
  }
}

class ConnectionListener() extends ConnectListener {
  override def onConnect(client: SocketIOClient): Unit = {
    println("Connected: " + client)
  }
}

class DisconnectionListener() extends DisconnectListener {
  override def onDisconnect(socket: SocketIOClient): Unit = {
    println("Disconnected: " + socket)
  }
}

class StopListener(server: Server) extends DataListener[Nothing] {
  override def onData(socket: SocketIOClient, data: Nothing, ackRequest: AckRequest): Unit = {
    server.server.getBroadcastOperations.sendEvent("server_stopped")
    println("stopping server")
    server.server.stop()
  }
}

