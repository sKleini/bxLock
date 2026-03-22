package de.kleini.bxlock

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.kleini.bxlock.databinding.ActivityAppBinding

class App : AppCompatActivity() {

    companion object {
        private const val TAG = "bxLock"
        private const val PREF_FILE = "bxlock_preferences"
        private const val PREF_KEY_IS_UNLOCKED = "is_unlocked"
    }

    private lateinit var binding: ActivityAppBinding
    private lateinit var prefs: SharedPreferences

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                Log.d(TAG, "Overlay permission granted")
                startLockTask()
            } else {
                Log.w(TAG, "Overlay permission denied by user")
                Toast.makeText(
                    this,
                    getString(R.string.permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_IS_UNLOCKED, false).apply()

        if (!requestDrawOverlayPermissionIfNeeded()) {
            startLockTask()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE)

        if (prefs.getBoolean(PREF_KEY_IS_UNLOCKED, false)) {
            Log.d(TAG, "Unlock requested, stopping lock task and finishing")
            if (!requestDrawOverlayPermissionIfNeeded()) {
                stopLockTask()
            }
            finishAndRemoveTask()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart")
        prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_IS_UNLOCKED, false).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun requestDrawOverlayPermissionIfNeeded(): Boolean {
        if (Settings.canDrawOverlays(this)) return false

        Log.w(TAG, "SYSTEM_ALERT_WINDOW permission missing, requesting from user")
        Toast.makeText(
            this,
            getString(R.string.permission_required),
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
        return true
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}
