package com.nothingcapsule.app.model

/**
 * Everything the capsule can currently be showing. Only one [CapsuleContent] is
 * "active" (driving the mini-capsule label/icon) at a time; the rest are queued
 * behind it and surface when the active one ends or the user swipes.
 *
 * Priority order when more than one is available at once (highest first):
 *   1. IncomingCall   – always wins, can't be dismissed by anything but answer/decline
 *   2. Timer           – countdown running
 *   3. Music           – something actively playing
 *   4. Notification    – whitelisted app posted something
 *   5. Battery         – only surfaces on charge-start / low-battery threshold, then times out
 */
sealed class CapsuleContent {

    data class Music(
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val packageName: String,
        val albumArtUri: String? = null,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L
    ) : CapsuleContent()

    data class Timer(
        val label: String,
        val remainingMs: Long,
        val totalMs: Long,
        val isPaused: Boolean = false
    ) : CapsuleContent()

    data class Battery(
        val percent: Int,
        val isCharging: Boolean,
        val isLow: Boolean
    ) : CapsuleContent()

    data class IncomingCall(
        val callerName: String,
        val callerNumber: String
    ) : CapsuleContent()

    data class Notification(
        val appName: String,
        val packageName: String,
        val title: String,
        val text: String,
        val postedAtMs: Long
    ) : CapsuleContent()

    object Idle : CapsuleContent()
}

enum class CapsuleExpansionState {
    COLLAPSED,   // small pill hugging the punch-hole
    EXPANDED     // full control panel
}
