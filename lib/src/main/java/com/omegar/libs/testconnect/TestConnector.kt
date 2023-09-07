package com.omegar.libs.testconnect

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.view.View
import com.omegar.libs.testconnect.SocketClient.Callback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * Created by Anton Knyazev on 30.08.2023.
 * Copyright (c) 2023 Omega https://omega-r.com
 */
internal object TestConnector : Callback, CoroutineScope {

    private var socketClient: SocketClient? = null
    private var activityCatcher: ActivityCatcher? = null
    private var logCatcher: LogCatcher? = null

    private var logJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    override val coroutineContext: CoroutineContext = Dispatchers.Default

    private val Context.appName: String
        get() {
            val applicationInfo = applicationInfo
            val stringId = applicationInfo.labelRes
            return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else getString(stringId)
        }

    private val Context.deviceName: String
        get() = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } +
                " " + Build.MODEL +
                " Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")" +
                userDeviceName?.let { " - $it" }?.replace(':', '-').orEmpty()

    private val Context.userDeviceName: String?
        get() = try {
            (if (VERSION.SDK_INT >= VERSION_CODES.N_MR1) {
                Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
            } else {
                null
            }) ?: run {
                Settings.System.getString(contentResolver, "device_name")
            } ?: run {
                Settings.Secure.getString(contentResolver, "bluetooth_name")
            } ?: run {
                Settings.System.getString(contentResolver, "bluetooth_name")
            }
        } catch (e: Throwable) {
            null
        }

    private val Context.appVersion: String
        get() {
            return try {
                val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
                val versionName = pInfo.versionName
                val versionCode = if (VERSION.SDK_INT >= VERSION_CODES.P) pInfo.longVersionCode else pInfo.versionCode.toLong()
                "$versionName ($versionCode)"
            } catch (e: NameNotFoundException) {
                e.printStackTrace()
                ""
            }
        }

    fun init(context: Context) {
        context.getServerAddress()?.let { serverAddress ->
            logCatcher = LogCatcher()
            socketClient = SocketClient(
                url = serverAddress,
                deviceName = context.deviceName,
                appName = context.appName,
                appVersion = context.appVersion,
                callback = this
            ).apply {
                connect()
            }
            activityCatcher = ActivityCatcher(context)
        }
    }

    private fun Context.getServerAddress(): String? {
        try {
            val serverAddressUri: Uri = OmegaTestConnectContract.CONTENT_URI
            val projection = arrayOf(OmegaTestConnectContract.COLUMN_SERVER_ADDRESS)
            val cursor = contentResolver.query(serverAddressUri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val serverAddress = cursor.getString(cursor.getColumnIndexOrThrow(OmegaTestConnectContract.COLUMN_SERVER_ADDRESS))
                cursor.close()
                return serverAddress
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }

    override fun startLog() {
        logJob = launch {
            logCatcher?.flow?.collect {
                socketClient?.sendLog(it)
            }
        }
    }

    override fun endLog() {
        logJob = null
    }

    override fun requestScreenshot() {
        socketClient?.let { socketClient ->
            activityCatcher
                ?.flow
                ?.value
                .getAllViews()
                .takeScreen()
                ?.let {
                    socketClient.sendScreenshot(it)
                    it.recycle()
                }
        }
    }

    @SuppressLint("PrivateApi")
    private fun Activity?.getAllViews(): List<View> {
        return try {
            val wmgClass = Class.forName("android.view.WindowManagerGlobal")
            val wmgInstance = wmgClass.getMethod("getInstance").invoke(null)
            val getViewRootNames = wmgClass.getMethod("getViewRootNames")
            val getRootView = wmgClass.getMethod("getRootView", String::class.java)
            val rootViewNames = getViewRootNames.invoke(wmgInstance) as Array<*>
            rootViewNames.mapNotNull { viewName ->
                getRootView.invoke(wmgInstance, viewName) as? View
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            this?.window?.decorView?.let { listOf(it) } ?: emptyList()
        }
    }

    private fun List<View>.takeScreen(): Bitmap? = runBlocking(Dispatchers.Main) {
        if (isEmpty()) return@runBlocking null
        try {
            val firstView = first()
            val bitmap = Bitmap.createBitmap(firstView.width, firstView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val locationOfViewInWindow = IntArray(2)

            forEach {
                it.getLocationOnScreen(locationOfViewInWindow)
                val saveCount = canvas.save()
                canvas.translate(locationOfViewInWindow[0].toFloat(), locationOfViewInWindow[1].toFloat())
                it.draw(canvas)
                canvas.restoreToCount(saveCount)
            }
            bitmap
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    object OmegaTestConnectContract {

        const val COLUMN_SERVER_ADDRESS = "server_address"
        const val AUTHORITY = "com.omega.testconnectprovider"
        val CONTENT_URI = Uri.parse("content://$AUTHORITY/server_address")
    }
}