import java.io.File

import com.corundumstudio.socketio.listener.{ConnectListener, DataListener, DisconnectListener}
import com.corundumstudio.socketio.{AckRequest, Configuration, SocketIOClient, SocketIOServer}

import scala.io._
import com.github.tototoshi.csv._

import scala.collection.mutable.ListBuffer

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

    println(data)
    

  }
}

class ratingListener() extends DataListener[String] {
  override def onData(socket: SocketIOClient, data: String, ackRequest: AckRequest): Unit = {
    val reader = CSVReader.open("../flask/static/data/TAlist.csv")
    val totalArr: ListBuffer[List[String]] = new ListBuffer[List[String]]()
    val dataArr = data.split("@")
    dataArr.foreach(a => println(a))
    val all = reader.all()
    for (line <- all) {
      if (line.head == dataArr.head) {
        val amount = (line(5).toDouble + 1).toString
        val total = (line(4).toDouble + dataArr(1).toDouble).toString
        val reviews = line(3) + ","+dataArr(2)
        val rating = (total.toDouble / amount.toDouble).toString
        socket.sendEvent("rating", rating)
        totalArr += List(dataArr.head, rating, line(2), reviews, total, amount)
      } else if (line.head == "NAME") {
        totalArr += List(line.head, line(1), line(2), line(3), line(4), line(5))
      }
      println("totalArr: " + totalArr)
    }
    val writer = CSVWriter.open("../flask/static/data/TAlist.csv")
    for (line <- totalArr) {
      writer.writeRow(List(line.head, line(1), line(2), line(3), line(4), line(5)))
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
    var data = ""
    for (map <- reader) {
      if (map("NAME") == inputData) {
        data += (map("NAME") + "$")
        data += (map("RATING") + "$")
        data += (map("CLASS") + "$")
        data += map("REVIEWS")
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

