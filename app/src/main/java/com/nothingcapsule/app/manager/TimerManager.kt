package com.nothingcapsule.app.manager

import android.os.CountDownTimer
import com.nothingcapsule.app.model.CapsuleContent

/**
 * One countdown at a time (matches how the mini-capsule can only show one
 * timer). If you need multiple concurrent timers, the expanded panel can list
 * them, but only the soonest-to-finish one drives the collapsed pill.
 */
class TimerManager(
    private val onUpdate: (CapsuleContent.Timer?) -> Unit,
    private val onFinished: (label: String) -> Unit
) {
    private var countDownTimer: CountDownTimer? = null
    private var label: String = ""
    private var totalMs: Long = 0L
    private var isPaused = false
    private var remainingAtPause: Long = 0L

    fun start(label: String, durationMs: Long) {
        cancel()
        this.label = label
        this.totalMs = durationMs
        this.isPaused = false
        runTicker(durationMs)
    }

    fun pause() {
        val timer = countDownTimer ?: return
        timer.cancel()
        isPaused = true
    }

    fun resume() {
        if (!isPaused) return
        isPaused = false
        runTicker(remainingAtPause)
    }

    fun cancel() {
        countDownTimer?.cancel()
        countDownTimer = null
        onUpdate(null)
    }

    private fun runTicker(fromMs: Long) {
        countDownTimer = object : CountDownTimer(fromMs, 250L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingAtPause = millisUntilFinished
                onUpdate(
                    CapsuleContent.Timer(
                        label = label,
                        remainingMs = millisUntilFinished,
                        totalMs = totalMs,
                        isPaused = false
                    )
                )
            }

            override fun onFinish() {
                onUpdate(null)
                onFinished(label)
            }
        }.start()
    }
}
