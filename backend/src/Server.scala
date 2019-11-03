import java.io.File

import com.corundumstudio.socketio.listener.{ConnectListener, DataListener, DisconnectListener}
import com.corundumstudio.socketio.{AckRequest, Configuration, SocketIOClient, SocketIOServer}

import scala.io.{BufferedSource, Source}
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
  server.addEventListener("getTA", classOf[String], new TAListener())
  server.addEventListener("addTA", classOf[String], new addTAListener())
  server.addEventListener("addRating", classOf[String], new ratingListener())
  server.addEventListener("chat_message", classOf[String], new MessageListener())
  server.addEventListener("stop_server", classOf[Nothing], new StopListener(this))

  server.start()
}

object Server {
  def main(args: Array[String]): Unit = {
    new Server()
  }
}

class addTAListener() extends DataListener[String] {
  override def onData(socket: SocketIOClient, data: String, ackRequest: AckRequest): Unit = {
    val writer = CSVWriter.open("../flask/static/data/TAlist.csv", append = true)
    val dataArr = data.split('$')
    writer.writeRow(List(dataArr(0), "n/a", dataArr(1), 0, 0, 0))
    writer.close()
  }
}

class ratingListener() extends DataListener[String] {
  override def onData(socket: SocketIOClient, data: String, ackRequest: AckRequest): Unit = {
    val reader = CSVReader.open(new File("../flask/static/data/TAlist.csv"))
    val it = reader.iterator
    val dataArr = data.split('&')
    while(it.hasNext) {
      var node = it.next.toArray
      if (node.head == dataArr.head) {
        node(5) += 1
        node(4) += dataArr(1)
        node(3) += (","+dataArr(2))
        node(1) = (node(4).toDouble / node(5).toDouble).toString
      }
    }
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

class TAListener() extends DataListener[String] {
  override def onData(socket: SocketIOClient, inputData: String, ackRequest: AckRequest): Unit = {
    val reader = CSVReader.open(new File("../flask/static/data/TAlist.csv")).allWithHeaders()
    println(reader)
    var data = ""
    for (map <- reader) {
      if (map("NAME") == inputData) {
        data += (map("NAME") + "$")
        data += (map("RATING") + "$")
        data += (map("COURSE") + "$")
        data += map("REVIEWS")
        println(data)
        socket.sendEvent("TA", data)
      }
    }
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


class MessageListener() extends DataListener[String] {
  override def onData(socket: SocketIOClient, data: String, ackRequest: AckRequest): Unit = {
    println("received message: " + data + " from " + socket)
    socket.sendEvent("ACK", "I received your message of " + data)
  }
}

class StopListener(server: Server) extends DataListener[Nothing] {
  override def onData(socket: SocketIOClient, data: Nothing, ackRequest: AckRequest): Unit = {
    server.server.getBroadcastOperations.sendEvent("server_stopped")
    println("stopping server")
    server.server.stop()
  }
}

