package com.example.barcodescanner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class TcpClient(
    private val serverIp: String,
    private val serverPort: Int
) {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            socket = Socket(serverIp, serverPort)
            writer = PrintWriter(socket!!.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            println("Connecté à $serverIp:$serverPort")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun sendMessage(message: String) = withContext(Dispatchers.IO) {
        writer?.println(message)
    }

    suspend fun receiveMessage(): String? = withContext(Dispatchers.IO) {
        return@withContext reader?.readLine()
    }

    // Écoute continue en arrière-plan
    fun startListening(scope: CoroutineScope, onMessage: (String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val message = reader?.readLine() ?: break
                    withContext(Dispatchers.Main) {
                        onMessage(message)
                    }
                }
            } catch (e: Exception) {
                println("Connexion fermée : ${e.message}")
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        reader?.close()
        writer?.close()
        socket?.close()
        println("Déconnecté")
    }
}

/*
fun main() = runBlocking{
    val client = TcpClient("198.168.2.51",12345)

    try{
        client.connect()

        client.startListening(this){message ->
            println("Message reçu : $message")
        }

        repeat(3) { i ->
            client.sendMessage("Message $i")
            delay(1000)
        }
    } finally {
        client.disconnect()
    }
}*/
