package com.nothingcapsule.app.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.nothingcapsule.app.model.CapsuleContent

/**
 * The capsule should NOT show battery status all the time — that's just noise.
 * It surfaces only:
 *   - the moment charging starts (and for a few seconds after),
 *   - when the level crosses the low-battery threshold while unplugged.
 * [CapsuleStateManager] handles the "only show if charging or low" filtering;
 * this class just reports the raw state on every change.
 */
class BatteryMonitor(
    private val context: Context,
    private val lowBatteryThreshold: Int = 20,
    private val onUpdate: (CapsuleContent.Battery) -> Unit
) {
    private var lastWasCharging = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

            val justStartedCharging = isCharging && !lastWasCharging
            lastWasCharging = isCharging

            onUpdate(
                CapsuleContent.Battery(
                    percent = percent,
                    isCharging = isCharging && (justStartedCharging || isCharging),
                    isLow = !isCharging && percent in 1..lowBatteryThreshold
                )
            )
        }
    }

    fun start() {
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    fun stop() {
        runCatching { context.unregisterReceiver(receiver) }
    }
}
