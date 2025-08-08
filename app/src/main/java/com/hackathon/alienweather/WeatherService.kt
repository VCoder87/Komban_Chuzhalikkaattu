package com.hackathon.alienweather

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.*

class WeatherService : Service() {

    companion object {
        const val CHANNEL_ID = "WeatherServiceChannel"
        const val NOTIFICATION_ID = 1
        private const val CHECK_INTERVAL = 30000L // 30 seconds
    }

    private var rainAlerts = false
    private var temperatureAlerts = false
    private var windAlerts = false

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var weatherAPI: PlanetaryWeatherAPI
    private lateinit var handler: Handler
    private lateinit var weatherCheckRunnable: Runnable

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        weatherAPI = PlanetaryWeatherAPI()
        handler = Handler(Looper.getMainLooper())

        createNotificationChannel()
        setupWeatherCheckRunnable()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Get alert preferences from intent
        rainAlerts = intent?.getBooleanExtra("RAIN_ALERTS", false) ?: false
        temperatureAlerts = intent?.getBooleanExtra("TEMPERATURE_ALERTS", false) ?: false
        windAlerts = intent?.getBooleanExtra("WIND_ALERTS", false) ?: false

        // Start foreground service
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start weather monitoring
        startWeatherMonitoring()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
    return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWeatherMonitoring()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Weather Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Alien Weather Alert")
                .setContentText("Monitoring planetary weather conditions...")
                .setSmallIcon(R.drawable.ic_weather_notification) // You'll need to add this icon
                .build()
    }

    private fun setupWeatherCheckRunnable() {
        weatherCheckRunnable = object : Runnable {
            override fun run() {
                checkWeatherConditions()
                handler.postDelayed(this, CHECK_INTERVAL)
            }
        }
    }

    private fun startWeatherMonitoring() {
        handler.post(weatherCheckRunnable)
    }

    private fun stopWeatherMonitoring() {
        handler.removeCallbacks(weatherCheckRunnable)
    }

    private fun checkWeatherConditions() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val weatherData = weatherAPI.getCurrentWeatherData()

                // Check for alert conditions
                if (rainAlerts && weatherData.rainfall > 50) {
                    notificationHelper.sendWeatherAlert(
                            "Heavy Rain Alert",
                            "Severe rainfall detected: ${weatherData.rainfall}mm/h"
                    )
                }

                if (temperatureAlerts && (weatherData.temperature > 45 || weatherData.temperature < -10)) {
                    notificationHelper.sendWeatherAlert(
                            "Extreme Temperature Alert",
                            "Temperature: ${weatherData.temperature}°C"
                    )
                }

                if (windAlerts && weatherData.windSpeed > 80) {
                    notificationHelper.sendWeatherAlert(
                            "High Wind Alert",
                            "Wind speed: ${weatherData.windSpeed} km/h"
                    )
                }

            } catch (e: Exception) {
                // Handle API errors
                notificationHelper.sendWeatherAlert(
                        "Weather Service Error",
                        "Failed to fetch weather data: ${e.message}"
                )
            }
        }
    }
}