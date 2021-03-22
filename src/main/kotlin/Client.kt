import java.io.*
import java.lang.Exception
import java.net.Socket
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    var address = "localhost"
    var port = 8080
    if (args.isNotEmpty()) {
        address = args[0]
        port = args[1].toInt()
    }

    try {
        var connection = Socket(address, port)
        println("Connected to Bob")
        var client = Client(connection)
        client.handle()
    } catch (e: Exception) {
        println("Connection failed")
    }
}

class Client(private val connection: Socket) {
    private val input = BufferedReader(InputStreamReader(connection.getInputStream()))
    private val output = BufferedWriter(OutputStreamWriter(connection.getOutputStream()))
    private val transmission = MessageTransmissionProtocol(connection)

    fun handle() {
        try {
            thread { transmission.SendingLoop() }
            thread { transmission.ReceivingLoop() }
        } catch (e : Exception) {
            println("Something went wrong")
        }
    }
}

