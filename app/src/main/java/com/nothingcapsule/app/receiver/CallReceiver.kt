package com.nothingcapsule.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.nothingcapsule.app.service.CapsuleOverlayService

/**
 * NOTE: `EXTRA_INCOMING_NUMBER` needs READ_CALL_LOG on some OEM skins in
 * addition to READ_PHONE_STATE — Nothing OS (stock-ish AOSP) does not require
 * it, but if you ever see a blank number here on another device, that's why.
 */
class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                CapsuleOverlayService.notifyIncomingCall(context, number ?: "Unknown")
            }
            TelephonyManager.EXTRA_STATE_IDLE,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                CapsuleOverlayService.clearIncomingCall(context)
            }
        }
    }
}
