package com.example.screen_lock_plugin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.EventChannel

class ScreenLockPlugin: FlutterPlugin, MethodCallHandler, ActivityAware,
    PluginRegistry.ActivityResultListener, EventChannel.StreamHandler {

    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var context: Context? = null
    private var activity: Activity? = null
    private var devicePolicyManager: DevicePolicyManager? = null
    private var componentName: ComponentName? = null
    private var pendingResult: Result? = null
    private var eventsSink: EventChannel.EventSink? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "ScreenLockPlugin"
        private const val DEVICE_ADMIN_REQUEST = 1001
        private const val EVENT_CHANNEL_NAME = "screen_lock_plugin/events"
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "screen_lock_plugin")
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, EVENT_CHANNEL_NAME)
        channel.setMethodCallHandler(this)
        eventChannel.setStreamHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "lockScreen" -> {
                lockScreen(result)
            }
            "isDeviceAdminEnabled" -> {
                result.success(isDeviceAdminEnabled())
            }
            "requestDeviceAdmin" -> {
                requestDeviceAdmin(result)
            }
            "isScreenOn" -> {
                result.success(isScreenOn())
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun lockScreen(result: Result) {
        if (devicePolicyManager == null || componentName == null) {
            result.error("NOT_INITIALIZED", "Device policy manager not initialized", null)
            return
        }

        if (devicePolicyManager!!.isAdminActive(componentName!!)) {
            try {
                devicePolicyManager!!.lockNow()
                result.success(true)
            } catch (e: SecurityException) {
                Log.e(TAG, "lockNow failed", e)
                result.error("LOCK_FAILED", e.message, null)
            }
        } else {
            result.success(false)
        }
    }

    private fun isScreenOn(): Boolean {
        val powerManager = context?.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager?.isInteractive ?: false
        } else {
            @Suppress("DEPRECATION")
            powerManager?.isScreenOn ?: false
        }
    }

    private fun isDeviceAdminEnabled(): Boolean {
        return if (devicePolicyManager != null && componentName != null) {
            devicePolicyManager!!.isAdminActive(componentName!!)
        } else {
            false
        }
    }

    private fun requestDeviceAdmin(result: Result) {
        if (activity == null) {
            result.error("NO_ACTIVITY", "Activity not available", null)
            return
        }

        if (componentName == null) {
            result.error("NOT_INITIALIZED", "Component name not initialized", null)
            return
        }

        pendingResult = result

        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Enable device admin to lock screen"
        )
        activity!!.startActivityForResult(intent, DEVICE_ADMIN_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == DEVICE_ADMIN_REQUEST) {
            pendingResult?.success(resultCode == Activity.RESULT_OK)
            pendingResult = null
            return true
        }
        return false
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        stopListeningForScreenEvents()
        context = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        devicePolicyManager = activity!!.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(activity!!, MyDeviceAdminReceiver::class.java)
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventsSink = events
        startListeningForScreenEvents()
    }

    override fun onCancel(arguments: Any?) {
        stopListeningForScreenEvents()
    }

    private fun startListeningForScreenEvents() {
        val appContext = context ?: return

        // Bridge events emitted by ScreenEventService into the active Dart
        // EventChannel. The broadcast receiver inside the service fires on the
        // main thread, but guard with a handler post to be safe in case a future
        // change moves it off-thread.
        ScreenEventService.listener = { event ->
            if (Looper.myLooper() == Looper.getMainLooper()) {
                eventsSink?.success(event)
            } else {
                mainHandler.post { eventsSink?.success(event) }
            }
        }

        val serviceIntent = Intent(appContext, ScreenEventService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent)
            } else {
                appContext.startService(serviceIntent)
            }
            Log.d(TAG, "Requested start of ScreenEventService")
        } catch (e: Exception) {
            // Most common cause: foreground-service-start restrictions. Log and
            // surface a best-effort error on the event stream so the Dart side
            // can react instead of silently missing events.
            Log.e(TAG, "Failed to start ScreenEventService", e)
            eventsSink?.error(
                "FGS_START_FAILED",
                "Failed to start screen-event foreground service: ${e.message}",
                null
            )
        }
    }

    private fun stopListeningForScreenEvents() {
        val appContext = context
        ScreenEventService.listener = null
        if (appContext != null) {
            try {
                appContext.stopService(Intent(appContext, ScreenEventService::class.java))
                Log.d(TAG, "Requested stop of ScreenEventService")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop ScreenEventService", e)
            }
        }
        eventsSink = null
    }
}