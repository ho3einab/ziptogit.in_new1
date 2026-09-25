package com.nothingcapsule.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nothingcapsule.app.CapsuleApplication
import com.nothingcapsule.app.CapsulePrefs
import com.nothingcapsule.app.model.CapsuleContent

class CapsuleNotificationListener : NotificationListenerService() {

    private val app get() = application as CapsuleApplication
    private val prefs by lazy { CapsulePrefs(this) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        CapsuleOverlayService.onNotificationAccessGranted(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg == packageName) return
        if (pkg !in prefs.whitelistedApps) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: return
        val text = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()
            ?: ""

        app.stateManager.updateNotification(
            CapsuleContent.Notification(
                appName = appLabelFor(pkg),
                packageName = pkg,
                title = title,
                text = text,
                postedAtMs = sbn.postTime
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName in prefs.whitelistedApps) {
            app.stateManager.clearNotification()
        }
    }

    private fun appLabelFor(pkg: String): String = runCatching {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}
