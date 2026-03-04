package com.example.barcodescanner

import kotlinx.coroutines.Dispatchers
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

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        reader?.close()
        writer?.close()
        socket?.close()
    }
}
