package com.nothingcapsule.app.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.nothingcapsule.app.R
import com.nothingcapsule.app.model.CapsuleContent
import com.nothingcapsule.app.model.CapsuleExpansionState
import kotlin.math.abs

/**
 * Root view added directly to the WindowManager. Two child layouts are
 * inflated once and toggled with visibility rather than re-inflating on every
 * state change, to keep the collapse/expand transition cheap and jank-free.
 */
class CapsuleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onTap: (() -> Unit)? = null
    var onSwipeDismiss: (() -> Unit)? = null

    // This view is always inflated FROM capsule_mini.xml, where it is itself the
    // root tag (<com.nothingcapsule.app.view.CapsuleView>) wrapping the two child
    // panels below as inline XML — so we grab references in onFinishInflate(),
    // once the XML-declared children actually exist, not in the constructor.
    private lateinit var miniIcon: ImageView
    private lateinit var miniLabel: TextView
    private lateinit var miniProgress: ProgressBar
    private lateinit var expandedRoot: View
    private lateinit var expandedTitle: TextView
    private lateinit var expandedSubtitle: TextView

    override fun onFinishInflate() {
        super.onFinishInflate()
        miniIcon = findViewById(R.id.capsule_icon)
        miniLabel = findViewById(R.id.capsule_label)
        miniProgress = findViewById(R.id.capsule_progress)
        expandedRoot = findViewById(R.id.capsule_expanded_root)
        expandedTitle = findViewById(R.id.capsule_expanded_title)
        expandedSubtitle = findViewById(R.id.capsule_expanded_subtitle)
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onTap?.invoke()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 != null && abs(e1.y - e2.y) > 60 && e2.y > e1.y) {
                onSwipeDismiss?.invoke()
                return true
            }
            return false
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    fun render(content: CapsuleContent, expansion: CapsuleExpansionState) {
        expandedRoot.visibility = if (expansion == CapsuleExpansionState.EXPANDED) VISIBLE else GONE
        findViewById<View>(R.id.capsule_mini_root).visibility =
            if (expansion == CapsuleExpansionState.EXPANDED) GONE else VISIBLE

        when (content) {
            is CapsuleContent.Music -> {
                miniIcon.setImageResource(if (content.isPlaying) R.drawable.ic_music_playing else R.drawable.ic_music_paused)
                miniLabel.text = "${content.title} · ${content.artist}"
                miniProgress.visibility = GONE
                expandedTitle.text = content.title
                expandedSubtitle.text = content.artist
            }
            is CapsuleContent.Timer -> {
                miniIcon.setImageResource(R.drawable.ic_timer)
                miniLabel.text = formatMillis(content.remainingMs)
                miniProgress.visibility = VISIBLE
                miniProgress.max = content.totalMs.toInt().coerceAtLeast(1)
                miniProgress.progress = (content.totalMs - content.remainingMs).toInt()
                expandedTitle.text = content.label
                expandedSubtitle.text = formatMillis(content.remainingMs)
            }
            is CapsuleContent.Battery -> {
                miniIcon.setImageResource(if (content.isCharging) R.drawable.ic_battery_charging else R.drawable.ic_battery_low)
                miniLabel.text = "${content.percent}%"
                miniProgress.visibility = GONE
                expandedTitle.text = context.getString(R.string.battery_title)
                expandedSubtitle.text = "${content.percent}%"
            }
            is CapsuleContent.IncomingCall -> {
                miniIcon.setImageResource(R.drawable.ic_call_incoming)
                miniLabel.text = content.callerName
                miniProgress.visibility = GONE
                expandedTitle.text = content.callerName
                expandedSubtitle.text = content.callerNumber
            }
            is CapsuleContent.Notification -> {
                miniIcon.setImageResource(R.drawable.ic_notification_generic)
                miniLabel.text = content.title
                miniProgress.visibility = GONE
                expandedTitle.text = content.appName
                expandedSubtitle.text = content.text
            }
            CapsuleContent.Idle -> {
                // Nothing to show — collapse to the smallest possible dot, no label.
                miniIcon.setImageResource(R.drawable.ic_capsule_dot)
                miniLabel.text = ""
                miniProgress.visibility = GONE
                expandedTitle.text = ""
                expandedSubtitle.text = ""
            }
        }
    }

    private fun formatMillis(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
