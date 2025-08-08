package com.hackathon.alienweather

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var rainSwitch: Switch
    private lateinit var temperatureSwitch: Switch
    private lateinit var windSwitch: Switch
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private var pendingStartAfterPermission: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        rainSwitch = findViewById(R.id.rainSwitch)
        temperatureSwitch = findViewById(R.id.temperatureSwitch)
        windSwitch = findViewById(R.id.windSwitch)
        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        // Set up button click listeners
        startButton.setOnClickListener {
            ensureNotificationPermissionThenStart()
        }

        stopButton.setOnClickListener {
            stopWeatherService()
        }
    }

    private fun ensureNotificationPermissionThenStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                pendingStartAfterPermission = true
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIFICATIONS
                )
                return
            }
        }
        startWeatherService()
    }

    private fun startWeatherService() {
        val intent = Intent(this, WeatherService::class.java)
        intent.putExtra("RAIN_ALERTS", rainSwitch.isChecked)
        intent.putExtra("TEMPERATURE_ALERTS", temperatureSwitch.isChecked)
        intent.putExtra("WIND_ALERTS", windSwitch.isChecked)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        statusText.text = "Weather monitoring started"
    }

    private fun stopWeatherService() {
        stopService(Intent(this, WeatherService::class.java))
        statusText.text = "Weather monitoring stopped"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingStartAfterPermission) {
                    pendingStartAfterPermission = false
                    startWeatherService()
                }
            } else {
                statusText.text = "Notification permission denied"
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }
}