package com.omegar.libs.testconnect

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import java.io.OutputStream
import java.io.PrintStream
import java.io.Reader
import java.io.StringWriter
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

/**
 * Created by Anton Knyazev on 07.07.2023.
 * Copyright (c) 2023 Omega https://omega-r.com
 */
internal class LogCatcher : Thread.UncaughtExceptionHandler {

    private val oldUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    private val uncaughtExceptionLogsFlow = MutableSharedFlow<String>()

    private val androidLogsFlow = callbackFlow {
        var process: Process? = null
        var reader: Reader? = null
        val thread = thread {
            try {
                process = Runtime.getRuntime().exec("logcat").apply {
                    reader = inputStream.bufferedReader().apply {
                        useLines {
                            it.forEach { line ->
                                trySendBlocking(line)
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                // Catch any exception occurred during the execution of the command
                cancel(e.toString(), e)
            }
        }

        awaitClose {
            // Close resources and stop the execution when the flow collector is cancelled
            thread.interrupt()
            process?.destroy()
            reader?.close()
        }

    }

    val flow: Flow<String> = merge(androidLogsFlow, uncaughtExceptionLogsFlow)
        .shareIn(GlobalScope, Companion.WhileSubscribed(10.seconds.inWholeMilliseconds))

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (uncaughtExceptionLogsFlow.subscriptionCount.value > 0) {
            e.printStackTrace(FlowPrintStream(uncaughtExceptionLogsFlow))
        }
        oldUncaughtExceptionHandler?.uncaughtException(t, e)
    }

    private class FlowPrintStream(val flow: MutableSharedFlow<String>): PrintStream(object : OutputStream() {
        override fun write(b: Int) {
            // nothing
        }
    }) {

        override fun println(x: String) {
            runBlocking {
                flow.emit(x)
            }
        }
    }

}