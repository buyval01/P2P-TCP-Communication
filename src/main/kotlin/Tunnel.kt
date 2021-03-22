import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.ServerSocket
import java.nio.charset.Charset
import java.util.Scanner
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    val tunnel = Tunnel(args[0].toInt(), args[1], args[2].toInt())
    tunnel.start()
}

class Tunnel(self_port: Int, private val dst_address: String, private var dst_port: Int) {
    private var server: ServerSocket = ServerSocket(self_port)

    fun start() {
        val lastAConnection: Socket? = null
        while (true) {
            if (lastAConnection == null || lastAConnection.isClosed) {
                val connectionA = server.accept()
                val connectionB = Socket(dst_address, dst_port)
                thread { listenA(connectionA, connectionB) }
                thread { listenB(connectionA, connectionB) }
            }
        }
    }

    private fun listenA(connectionA: Socket, connectionB: Socket) {
        val inputA = Scanner(connectionA.getInputStream())
        val outputB = connectionB.getOutputStream()
        while (true) {
            try {
                val message = inputA.nextLine() ?: ""
                println("From Alise to Bob: $message")
                outputB.write("${message}\n".toByteArray(Charset.defaultCharset()));
            } catch (e: Exception) {
                connectionA.close()
                connectionB.close()
                break
            }
        }
    }

    private fun listenB(connectionA: Socket, connectionB: Socket) {
        val inputB = Scanner(connectionB.getInputStream())
        val outputA = connectionA.getOutputStream()
        while (true) {
            try {
                val message = inputB.nextLine() ?: ""
                println("From Bob to Alise: $message")
                outputA.write("${message}\n".toByteArray(Charset.defaultCharset()))
            } catch (e: Exception) {
                connectionA.close()
                connectionB.close()
                break
            }
        }
    }
}
