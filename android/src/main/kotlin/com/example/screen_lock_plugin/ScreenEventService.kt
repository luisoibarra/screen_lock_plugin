package com.example.screen_lock_plugin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.Display

class ScreenEventService : Service() {

    private var receiver: BroadcastReceiver? = null
    private var displayManager: DisplayManager? = null
    private var displayListener: DisplayManager.DisplayListener? = null
    private var lastInteractive: Boolean? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pollTask = object : Runnable {
        override fun run() {
            emitFromPowerManager("poll")
            mainHandler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    companion object {
        private const val TAG = "ScreenLockPlugin"
        private const val CHANNEL_ID = "screen_lock_plugin_screen_events"
        private const val CHANNEL_NAME = "Screen state monitoring"
        private const val NOTIF_ID = 3127
        private const val POLL_INTERVAL_MS = 2000L

        const val EVENT_SCREEN_ON = "SCREEN_ON"
        const val EVENT_SCREEN_OFF = "SCREEN_OFF"

        // Set by ScreenLockPlugin while a Dart EventChannel listener is active.
        // Invoked on the main thread by this service.
        @Volatile
        var listener: ((String) -> Unit)? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ScreenEventService.onCreate")
        ensureNotificationChannel()
        startInForeground()
        registerScreenReceiver()
        registerDisplayListener()
        // Seed the state so later transitions emit, and start the fallback poll.
        emitFromPowerManager("onCreate")
        mainHandler.postDelayed(pollTask, POLL_INTERVAL_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ScreenEventService.onStartCommand flags=$flags startId=$startId")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "ScreenEventService.onDestroy")
        mainHandler.removeCallbacks(pollTask)
        unregisterDisplayListener()
        val r = receiver
        if (r != null) {
            try {
                unregisterReceiver(r)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver was not registered", e)
            }
        }
        receiver = null
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the app alive to observe screen on/off events."
                setShowBadge(false)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen monitor active")
                .setContentText("Monitoring screen on/off state.")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Screen monitor active")
                .setContentText("Monitoring screen on/off state.")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .build()
        }
    }

    private fun startInForeground() {
        val notification = buildNotification()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val type = resolveForegroundServiceType()
                Log.d(TAG, "startForeground type=$type (API ${Build.VERSION.SDK_INT})")
                startForeground(NOTIF_ID, notification, type)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                startForeground(
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            }
            else -> {
                startForeground(NOTIF_ID, notification)
            }
        }
    }

    // On kiosk devices provisioned as Device Owner, SYSTEM_EXEMPTED gives us
    // the broadest exemption from background/FGS restrictions. For non-
    // device-owner apps we fall back to SPECIAL_USE (matches
    // PROPERTY_SPECIAL_USE_FGS_SUBTYPE in the manifest).
    private fun resolveForegroundServiceType(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val isDeviceOwner = try {
            dpm?.isDeviceOwnerApp(packageName) == true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query device-owner status", e)
            false
        }
        return if (isDeviceOwner) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        }
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                Log.d(TAG, "ScreenEventService.onReceive action=$action")
                when (action) {
                    Intent.ACTION_SCREEN_ON -> emitScreenState(true, "broadcast")
                    Intent.ACTION_SCREEN_OFF -> emitScreenState(false, "broadcast")
                }
            }
        }
        receiver = r
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(r, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(r, filter)
        }
    }

    // Secondary signal: DisplayManager reports STATE_ON, STATE_OFF, STATE_DOZE,
    // and STATE_DOZE_SUSPEND changes. This catches AOD transitions on devices
    // that do not broadcast ACTION_SCREEN_OFF for short power presses and is
    // resilient to broadcast throttling.
    private fun registerDisplayListener() {
        val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return
        displayManager = dm
        val l = object : DisplayManager.DisplayListener {
            override fun onDisplayChanged(displayId: Int) {
                if (displayId != Display.DEFAULT_DISPLAY) return
                val state = dm.getDisplay(displayId)?.state
                emitFromPowerManager("displayChanged state=$state")
            }

            override fun onDisplayAdded(displayId: Int) {}
            override fun onDisplayRemoved(displayId: Int) {}
        }
        displayListener = l
        dm.registerDisplayListener(l, mainHandler)
    }

    private fun unregisterDisplayListener() {
        val dm = displayManager
        val l = displayListener
        if (dm != null && l != null) {
            try {
                dm.unregisterDisplayListener(l)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister DisplayListener", e)
            }
        }
        displayManager = null
        displayListener = null
    }

    private fun emitFromPowerManager(source: String) {
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        val interactive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            pm.isInteractive
        } else {
            @Suppress("DEPRECATION")
            pm.isScreenOn
        }
        emitScreenState(interactive, source)
    }

    // All three inputs (broadcast / display / poll) funnel through here so the
    // Dart side only ever sees real transitions, never duplicates.
    private fun emitScreenState(interactive: Boolean, source: String) {
        if (lastInteractive == interactive) return
        lastInteractive = interactive
        val event = if (interactive) EVENT_SCREEN_ON else EVENT_SCREEN_OFF
        Log.d(TAG, "emit $event from=$source")
        listener?.invoke(event)
    }
}
