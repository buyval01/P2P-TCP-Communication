import java.io.*
import java.lang.Exception
import java.net.Socket
import java.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class Security {
    lateinit var publickey : PublicKey
        private set
    lateinit var sharedSecret : ByteArray
    private lateinit var keyAgreement : KeyAgreement

    private val cryptoAlgo = "DiffieHellman"
    private val cipherAlgo = "AES"

    init {
        GenerateKeys()
    }

    private fun GenerateKeys() {
        val generator = KeyPairGenerator.getInstance(cryptoAlgo)
        generator.initialize(512)
        val keys = generator.genKeyPair()
        publickey = keys.public
        keyAgreement = KeyAgreement.getInstance(cryptoAlgo)
        keyAgreement.init(keys.private)
    }

    fun GetPublicKeyString() : String {
        val byteArrayKey = publickey.encoded
        return Base64.getEncoder().encodeToString(byteArrayKey)
    }

    fun GenerateSharedSecret(publickeyString: String) {
        val factory = KeyFactory.getInstance(cryptoAlgo)
        val byteArrayKey = Base64.getDecoder().decode(publickeyString)
        val anotherPublicKey = factory.generatePublic(X509EncodedKeySpec(byteArrayKey)) as DHPublicKey
        keyAgreement.doPhase(anotherPublicKey, true)
        sharedSecret = keyAgreement.generateSecret()
    }

    var lastOffset = 0
    fun Encrypt(message : String, offset : Int = lastOffset): String {
        lastOffset = offset
        val key = SecretKeySpec(sharedSecret, lastOffset, 32, cipherAlgo)
        val cipher = Cipher.getInstance(cipherAlgo)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encVal = cipher.doFinal(message.toByteArray())
        return Base64.getEncoder().encodeToString(encVal)
    }

    fun Decrypt(encryptedMessage : String, offset: Int = lastOffset) : String {
        lastOffset = offset
        val key = SecretKeySpec(sharedSecret, lastOffset, 32, cipherAlgo)
        val cipher = Cipher.getInstance(cipherAlgo)
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decordedValue: ByteArray = Base64.getDecoder().decode(encryptedMessage)
        val decValue = cipher.doFinal(decordedValue)
        return String(decValue)
    }
}

class MessageTransmissionProtocol (val connection : Socket) {
    private val cipherManager = Security()
    private val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
    private val writer = BufferedWriter(OutputStreamWriter(connection.getOutputStream()))
    private var companionKey = false

    enum class MessageType {
        MESSAGE,
        KEY,
    }

    fun ReceivingLoop() {
        while (!connection.isClosed) {
            try {
                val message = ReceiveMessage()
                if (message != "") {
                    println(message)
                }
            } catch (e: Exception) {
                connection.close()
                break
            }
        }
    }

    fun SendingLoop() {
        SendMessage(MessageType.KEY, cipherManager.GetPublicKeyString())
        Thread.sleep(1000L)
        if (!companionKey) {
            println("I don't trust my companion!")
            connection.close()
            return
        }
        while (!connection.isClosed) {
            val message = readLine() ?: ""
            if (message != "") {
                try {
                    SendMessage(MessageType.MESSAGE, message)
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun SendMessage(type : MessageType, message : String) {
        writer.write("${type.toString()}\n")
        writer.flush()
        when(type) {
            MessageType.KEY -> writer.write("$message\n")
            MessageType.MESSAGE -> {
                val cipherOffset = Random.nextInt(0, 31)
                writer.write("${cipherManager.Encrypt(cipherOffset.toString())}\n")
                writer.flush()
                writer.write("${cipherManager.Encrypt(message, cipherOffset)}\n")
            }
        }
        writer.flush()
    }

    private fun ReceiveMessage() : String {
        val type = reader.readLine() ?: throw Exception()
        return when(type) {
            MessageType.MESSAGE.toString() -> {
                val encryptedOffset = reader.readLine() ?: throw Exception()
                val offset = cipherManager.Decrypt(encryptedOffset).toInt()
                val message = reader.readLine() ?: throw Exception()
                cipherManager.Decrypt(message, offset)
            }
            MessageType.KEY.toString() -> {
                val message = reader.readLine() ?: throw Exception()
                companionKey = true
                cipherManager.GenerateSharedSecret(message)
                ""
            }
            else -> ""
        }
    }
}
