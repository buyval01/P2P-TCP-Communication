import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    val port = if (args.isNotEmpty()) {
        args[0].toInt()
    } else {
        8080
    }
    val server = Server(port)
    server.start()
}

class Server(port: Int) {
    private val serverSocket = ServerSocket(port)
    private var transmision: MessageTransmissionProtocol? = null
    private var connected = false


    fun start() {
        var lastConnection: Socket? = null
        while (true) {
            if (lastConnection == null || lastConnection.isClosed) {
                if (lastConnection != null) println("Alice disconnected")
                lastConnection = serverSocket.accept()
                println("Alice connected")
                transmision = MessageTransmissionProtocol(lastConnection)
                try {
                    thread { transmision!!.SendingLoop() }
                    thread { transmision!!.ReceivingLoop() }
                } catch (e : Exception) {
                    println("Something went wrong")
                }
            }
        }
    }
}
