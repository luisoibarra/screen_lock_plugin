package com.example.screen_lock_plugin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

class ScreenLockPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, 
    PluginRegistry.ActivityResultListener {
    
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var devicePolicyManager: DevicePolicyManager? = null
    private var componentName: ComponentName? = null
    private var pendingResult: Result? = null

    companion object {
        private const val DEVICE_ADMIN_REQUEST = 1001
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "screen_lock_plugin")
        channel.setMethodCallHandler(this)
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
            devicePolicyManager!!.lockNow()
            result.success(true)
        } else {
            result.success(false)
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
}