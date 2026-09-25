package com.nothingcapsule.app

import android.app.Application
import com.nothingcapsule.app.manager.CapsuleStateManager

class CapsuleApplication : Application() {

    /** Shared across the overlay service, notification listener, and any UI that needs it. */
    val stateManager by lazy { CapsuleStateManager() }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CapsuleApplication
            private set
    }
}
