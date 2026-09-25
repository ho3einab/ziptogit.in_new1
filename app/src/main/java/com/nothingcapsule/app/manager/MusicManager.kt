package com.nothingcapsule.app.manager

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.nothingcapsule.app.model.CapsuleContent
import com.nothingcapsule.app.service.CapsuleNotificationListener

/**
 * Watches every active [MediaController] on the device (Spotify, YouTube Music,
 * the system player, etc.) and reports the most recently active one that is
 * currently playing.
 *
 * Requires notification-listener access — Android only hands out the list of
 * active media sessions to apps that are also a bound NotificationListenerService,
 * which is why [CapsuleNotificationListener] doubles as the entry point here.
 */
class MusicManager(
    private val context: Context,
    private val onUpdate: (CapsuleContent.Music?) -> Unit
) {
    private val tag = "MusicManager"

    private val sessionManager by lazy {
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publish(activeController)
        }

        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            publish(activeController)
        }

        override fun onSessionDestroyed() {
            activeController = null
            onUpdate(null)
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            attachToBestController(controllers)
        }

    /** Call once notification-listener permission is confirmed granted. */
    fun start() {
        val componentName = ComponentName(context, CapsuleNotificationListener::class.java)
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            attachToBestController(sessionManager.getActiveSessions(componentName))
        } catch (e: SecurityException) {
            Log.w(tag, "Notification listener access not granted yet", e)
        }
    }

    fun stop() {
        sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
    }

    private fun attachToBestController(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)

        val best = controllers
            ?.filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?.firstOrNull()
            ?: controllers?.firstOrNull()

        activeController = best
        best?.registerCallback(controllerCallback)
        publish(best)
    }

    private fun publish(controller: MediaController?) {
        if (controller == null) {
            onUpdate(null)
            return
        }
        val metadata = controller.metadata
        val state = controller.playbackState

        if (metadata == null || state == null) {
            onUpdate(null)
            return
        }

        onUpdate(
            CapsuleContent.Music(
                title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
                artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "",
                isPlaying = state.state == PlaybackState.STATE_PLAYING,
                packageName = controller.packageName,
                positionMs = state.position,
                durationMs = metadata.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)
            )
        )
    }

    fun togglePlayPause() {
        val controller = activeController ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipNext() = activeController?.transportControls?.skipToNext()
    fun skipPrevious() = activeController?.transportControls?.skipToPrevious()
}
