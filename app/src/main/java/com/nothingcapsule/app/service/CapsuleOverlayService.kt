package com.nothingcapsule.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.nothingcapsule.app.CapsuleApplication
import com.nothingcapsule.app.CapsulePrefs
import com.nothingcapsule.app.MainActivity
import com.nothingcapsule.app.R
import com.nothingcapsule.app.manager.BatteryMonitor
import com.nothingcapsule.app.manager.MusicManager
import com.nothingcapsule.app.manager.TimerManager
import com.nothingcapsule.app.model.CapsuleContent
import com.nothingcapsule.app.model.CapsuleExpansionState
import com.nothingcapsule.app.view.CapsuleView
import kotlinx.coroutines.launch

class CapsuleOverlayService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var capsuleView: CapsuleView
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var isAdded = false

    private lateinit var prefs: CapsulePrefs
    private val app get() = application as CapsuleApplication

    private val musicManager by lazy {
        MusicManager(applicationContext) { app.stateManager.updateMusic(it) }
    }
    private var batteryMonitor: BatteryMonitor? = null
    private val timerManager = TimerManager(
        onUpdate = { app.stateManager.updateTimer(it) },
        onFinished = { label -> /* TODO: fire a distinct "timer done" pulse + haptic */ }
    )

    private var expansionState = CapsuleExpansionState.COLLAPSED

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = CapsulePrefs(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        setupOverlayView()
        observeState()
        startSources()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_INCOMING_CALL -> {
                val number = intent.getStringExtra(EXTRA_CALLER) ?: "Unknown"
                app.stateManager.updateCall(CapsuleContent.IncomingCall(callerName = number, callerNumber = number))
            }
            ACTION_CLEAR_CALL -> app.stateManager.updateCall(null)
            ACTION_START_TIMER -> {
                val label = intent.getStringExtra(EXTRA_TIMER_LABEL) ?: "Timer"
                val durationMs = intent.getLongExtra(EXTRA_TIMER_DURATION_MS, 60_000L)
                timerManager.start(label, durationMs)
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun setupOverlayView() {
        capsuleView = LayoutInflater.from(this).inflate(R.layout.capsule_mini, null) as CapsuleView

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(12)
        }

        capsuleView.onTap = { toggleExpansion() }
        capsuleView.onSwipeDismiss = { collapseAndHideTransient() }

        windowManager.addView(capsuleView, layoutParams)
        isAdded = true

        capsuleView.post { positionAroundCutout() }
    }

    private fun positionAroundCutout() {
        val insets = capsuleView.rootWindowInsets ?: return
        val cutout = insets.displayCutout ?: return
        val holeRect = cutout.boundingRects.firstOrNull() ?: return

        val holeCenterY = holeRect.top + holeRect.height() / 2
        val pillHeight = capsuleView.height.takeIf { it > 0 } ?: dpToPx(32)

        layoutParams.y = (holeCenterY - pillHeight / 2).coerceAtLeast(dpToPx(4))
        runCatching { windowManager.updateViewLayout(capsuleView, layoutParams) }
    }

    private fun observeState() {
        lifecycleScope.launch {
            app.stateManager.activeContent.collect { content ->
                capsuleView.render(content, expansionState)
            }
        }
    }

    private fun startSources() {
        batteryMonitor = BatteryMonitor(
            context = this,
            lowBatteryThreshold = prefs.lowBatteryThreshold,
            onUpdate = { app.stateManager.updateBattery(it) }
        ).also { it.start() }
    }

    private fun toggleExpansion() {
        expansionState = if (expansionState == CapsuleExpansionState.COLLAPSED) {
            CapsuleExpansionState.EXPANDED
        } else {
            CapsuleExpansionState.COLLAPSED
        }
        capsuleView.render(app.stateManager.activeContent.value, expansionState)
        capsuleView.post { positionAroundCutout() }
    }

    private fun collapseAndHideTransient() {
        expansionState = CapsuleExpansionState.COLLAPSED
        app.stateManager.clearNotification()
    }

    private fun buildForegroundNotification(): android.app.Notification {
        val channelId = "capsule_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                channelId,
                getString(R.string.capsule_service_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_capsule_notification)
            .setContentTitle(getString(R.string.capsule_running_title))
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        if (isAdded) runCatching { windowManager.removeView(capsuleView) }
        musicManager.stop()
        batteryMonitor?.stop()
        timerManager.cancel()
    }

    fun attachMusicManager() = musicManager.start()

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        private var instance: CapsuleOverlayService? = null

        private const val NOTIFICATION_ID = 1001

        private const val ACTION_INCOMING_CALL = "com.nothingcapsule.app.action.INCOMING_CALL"
        private const val ACTION_CLEAR_CALL = "com.nothingcapsule.app.action.CLEAR_CALL"
        private const val ACTION_START_TIMER = "com.nothingcapsule.app.action.START_TIMER"
        private const val ACTION_STOP = "com.nothingcapsule.app.action.STOP"

        private const val EXTRA_CALLER = "extra_caller"
        private const val EXTRA_TIMER_LABEL = "extra_timer_label"
        private const val EXTRA_TIMER_DURATION_MS = "extra_timer_duration_ms"

        fun start(context: Context) {
            val intent = Intent(context, CapsuleOverlayService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, CapsuleOverlayService::class.java).setAction(ACTION_STOP))
        }

        fun notifyIncomingCall(context: Context, number: String) {
            val intent = Intent(context, CapsuleOverlayService::class.java)
                .setAction(ACTION_INCOMING_CALL)
                .putExtra(EXTRA_CALLER, number)
            ContextCompat.startForegroundService(context, intent)
        }

        fun clearIncomingCall(context: Context) {
            context.startService(Intent(context, CapsuleOverlayService::class.java).setAction(ACTION_CLEAR_CALL))
        }

        fun startTimer(context: Context, label: String, durationMs: Long) {
            val intent = Intent(context, CapsuleOverlayService::class.java)
                .setAction(ACTION_START_TIMER)
                .putExtra(EXTRA_TIMER_LABEL, label)
                .putExtra(EXTRA_TIMER_DURATION_MS, durationMs)
            ContextCompat.startForegroundService(context, intent)
        }

        fun onNotificationAccessGranted(context: Context) {
            instance?.attachMusicManager() ?: start(context)
        }
    }
}
