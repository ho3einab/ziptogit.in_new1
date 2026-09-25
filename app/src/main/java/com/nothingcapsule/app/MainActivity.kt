package com.nothingcapsule.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.nothingcapsule.app.service.CapsuleOverlayService

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var prefs: CapsulePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = CapsulePrefs(this)

        statusText = findViewById(R.id.status_text)
        toggleButton = findViewById(R.id.btn_toggle_capsule)
        val permissionsButton = findViewById<Button>(R.id.btn_setup_permissions)

        toggleButton.setOnClickListener { onToggleClicked() }
        permissionsButton.setOnClickListener {
            startActivity(Intent(this, PermissionActivity::class.java))
        }
        findViewById<Button>(R.id.btn_pick_apps).setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun onToggleClicked() {
        if (!PermissionActivity.allPermissionsGranted(this)) {
            startActivity(Intent(this, PermissionActivity::class.java))
            return
        }

        if (prefs.isCapsuleEnabled) {
            prefs.isCapsuleEnabled = false
            CapsuleOverlayService.stop(this)
        } else {
            prefs.isCapsuleEnabled = true
            CapsuleOverlayService.start(this)
        }
        refreshStatus()
    }

    private fun refreshStatus() {
        val granted = PermissionActivity.allPermissionsGranted(this)
        val running = prefs.isCapsuleEnabled
        statusText.text = when {
            !granted -> "Permissions needed before you can turn this on"
            running -> "Running"
            else -> "Off"
        }
        toggleButton.text = getString(
            if (running) R.string.disable_capsule else R.string.enable_capsule
        )
    }
}
