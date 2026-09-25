package com.nothingcapsule.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nothingcapsule.app.CapsulePrefs
import com.nothingcapsule.app.service.CapsuleOverlayService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (CapsulePrefs(context).isCapsuleEnabled) {
            CapsuleOverlayService.start(context)
        }
    }
}
