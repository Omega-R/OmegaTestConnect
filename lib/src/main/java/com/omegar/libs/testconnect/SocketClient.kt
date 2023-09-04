package com.omegar.libs.testconnect

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.ByteArrayOutputStream
import java.net.URI
import kotlin.coroutines.CoroutineContext

/**
 * Created by Anton Knyazev on 11.07.2023.
 * Copyright (c) 2023 Omega https://omega-r.com
 */
internal class SocketClient(
    url: String,
    private val deviceName: String,
    private val appName: String,
    private val appVersion: String,
    private val callback: Callback
) : WebSocketClient(URI(url)), CoroutineScope {

    private companion object {
        const val HEADER_NAME = "NAME"
        const val HEADER_LOG_START = "LOG_START"
        const val HEADER_LOG = "LOG"
        const val HEADER_LOG_END = "LOG_END"
        const val HEADER_REQUEST_SCREENSHOT = "SCREENSHOT"
        const val HEADER_SEND_SCREENSHOT = "SCREENSHOT"
    }

    private var reconnectJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    override val coroutineContext: CoroutineContext = Dispatchers.Default

    override fun onOpen(handshakedata: ServerHandshake?) {
        send("$HEADER_NAME:$deviceName:$appName:$appVersion")
    }

    override fun onMessage(message: String?) {
        when (message) {
            HEADER_LOG_START -> callback.startLog()
            HEADER_LOG_END -> callback.endLog()
            HEADER_REQUEST_SCREENSHOT -> callback.requestScreenshot()
        }
    }

    fun sendLog(logText: String) {
        if (!isClosed) {
            send("$HEADER_LOG:$logText")
        }
    }

    fun sendScreenshot(bitmap: Bitmap) {
        if (!isClosed) {
            val stream = ByteArrayOutputStream()
            stream.write("$HEADER_SEND_SCREENSHOT:".toByteArray())
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            send(stream.toByteArray())
        }
    }

    override fun onError(ex: Exception?) {
        // nothing
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        waitAndReconnect()
    }

    private fun waitAndReconnect() {
        reconnectJob = launch {
            delay(2000)
            reconnect()
        }
    }

    interface Callback {

        fun startLog()

        fun endLog()

        fun requestScreenshot()
    }
}