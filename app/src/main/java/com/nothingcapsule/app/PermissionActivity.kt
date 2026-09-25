package com.nothingcapsule.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class PermissionActivity : AppCompatActivity() {

    private val postNotificationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshRows() }

    private val phoneStateLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshRows() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        bindRow(
            containerId = R.id.row_overlay,
            title = getString(R.string.permission_overlay_title),
            desc = getString(R.string.permission_overlay_desc)
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        bindRow(
            containerId = R.id.row_notification_listener,
            title = getString(R.string.permission_notification_title),
            desc = getString(R.string.permission_notification_desc)
        ) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        bindRow(
            containerId = R.id.row_post_notification,
            title = getString(R.string.permission_post_notification_title),
            desc = getString(R.string.permission_post_notification_desc)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        bindRow(
            containerId = R.id.row_phone_state,
            title = getString(R.string.permission_phone_state_title),
            desc = getString(R.string.permission_phone_state_desc)
        ) {
            phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRows()
    }

    private fun bindRow(containerId: Int, title: String, desc: String, onClick: () -> Unit) {
        val container = findViewById<View>(containerId)
        container.findViewById<TextView>(R.id.row_title).text = title
        container.findViewById<TextView>(R.id.row_desc).text = desc
        container.findViewById<Button>(R.id.row_button).setOnClickListener { onClick() }
    }

    private fun refreshRows() {
        setRowGranted(R.id.row_overlay, hasOverlayPermission(this))
        setRowGranted(R.id.row_notification_listener, hasNotificationListenerAccess(this))
        setRowGranted(R.id.row_post_notification, hasPostNotificationPermission(this))
        setRowGranted(R.id.row_phone_state, hasPhoneStatePermission(this))
    }

    private fun setRowGranted(containerId: Int, granted: Boolean) {
        val button = findViewById<View>(containerId).findViewById<Button>(R.id.row_button)
        button.isEnabled = !granted
        button.text = if (granted) "✓ Granted" else getString(R.string.grant_button)
    }

    companion object {
        fun allPermissionsGranted(context: Context): Boolean =
            hasOverlayPermission(context) &&
                hasNotificationListenerAccess(context) &&
                hasPostNotificationPermission(context) &&
                hasPhoneStatePermission(context)

        private fun hasOverlayPermission(context: Context): Boolean =
            Settings.canDrawOverlays(context)

        private fun hasNotificationListenerAccess(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        private fun hasPostNotificationPermission(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        private fun hasPhoneStatePermission(context: Context): Boolean =
            context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
