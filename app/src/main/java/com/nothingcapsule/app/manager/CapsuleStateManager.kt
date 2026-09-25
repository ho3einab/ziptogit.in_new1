package com.nothingcapsule.app.manager

import com.nothingcapsule.app.model.CapsuleContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for "what should the capsule show right now".
 * Each source manager (music, timer, battery, call, notifications) pushes its
 * own latest value in here; this class resolves the priority and exposes one
 * merged [StateFlow] that the overlay service/view collects.
 *
 * This is intentionally NOT a singleton via `object` — it's constructed once by
 * CapsuleApplication and handed to whoever needs it, so it's easy to swap out
 * in tests.
 */
class CapsuleStateManager {

    private var incomingCall: CapsuleContent.IncomingCall? = null
    private var timer: CapsuleContent.Timer? = null
    private var music: CapsuleContent.Music? = null
    private var notification: CapsuleContent.Notification? = null
    private var battery: CapsuleContent.Battery? = null

    private val _activeContent = MutableStateFlow<CapsuleContent>(CapsuleContent.Idle)
    val activeContent: StateFlow<CapsuleContent> = _activeContent.asStateFlow()

    fun updateCall(call: CapsuleContent.IncomingCall?) {
        incomingCall = call
        recompute()
    }

    fun updateTimer(t: CapsuleContent.Timer?) {
        timer = t
        recompute()
    }

    fun updateMusic(m: CapsuleContent.Music?) {
        // Don't show a "paused" music card just because the user paused it minutes
        // ago and moved on; only keep it while playing or very recently touched.
        music = m?.takeIf { it.isPlaying } ?: m?.takeIf { !it.isPlaying && music?.packageName == it.packageName }
        recompute()
    }

    fun updateNotification(n: CapsuleContent.Notification?) {
        notification = n
        recompute()
    }

    fun updateBattery(b: CapsuleContent.Battery?) {
        battery = b
        recompute()
    }

    fun clearNotification() {
        notification = null
        recompute()
    }

    private fun recompute() {
        _activeContent.value =
            incomingCall
                ?: timer
                ?: music
                ?: notification
                ?: battery?.takeIf { it.isCharging || it.isLow }
                ?: CapsuleContent.Idle
    }
}
