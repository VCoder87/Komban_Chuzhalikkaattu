package com.hackathon.alienweather

import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

data class WeatherData(
    val temperature: Double,
    val rainfall: Double,
    val windSpeed: Double,
    val humidity: Double,
    val pressure: Double,
    val timestamp: Long = System.currentTimeMillis()
)

class PlanetaryWeatherAPI {

    companion object {
        // Replace with your actual weather API endpoint
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"
        private const val API_KEY = "YOUR_API_KEY_HERE" // Replace with your API key
        private const val CITY = "London" // Default city
    }

    suspend fun getCurrentWeatherData(): WeatherData {
        return try {
            // Add delay to simulate API call
            delay(1000)

            // For demonstration, we'll use simulated data
            // In a real app, you would make an actual HTTP request
            getSimulatedWeatherData()

            // Uncomment the line below and comment out the line above for real API calls
            // getRealWeatherData()

        } catch (e: Exception) {
            throw Exception("Failed to fetch weather data: ${e.message}")
        }
    }

    private fun getSimulatedWeatherData(): WeatherData {
        // Simulate alien planet weather with more extreme conditions
        return WeatherData(
            temperature = Random.nextDouble(-20.0, 60.0),
            rainfall = Random.nextDouble(0.0, 100.0),
            windSpeed = Random.nextDouble(0.0, 150.0),
            humidity = Random.nextDouble(20.0, 100.0),
            pressure = Random.nextDouble(900.0, 1100.0)
        )
    }

    private suspend fun getRealWeatherData(): WeatherData {
        val urlString = "$BASE_URL?q=$CITY&appid=$API_KEY&units=metric"
        val url = URL(urlString)

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        return try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                parseWeatherResponse(response)
            } else {
                throw Exception("HTTP Error: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseWeatherResponse(jsonResponse: String): WeatherData {
        val jsonObject = JSONObject(jsonResponse)
        val main = jsonObject.getJSONObject("main")
        val wind = jsonObject.getJSONObject("wind")
        val rain = jsonObject.optJSONObject("rain")

        return WeatherData(
            temperature = main.getDouble("temp"),
            rainfall = rain?.optDouble("1h", 0.0) ?: 0.0,
            windSpeed = wind.getDouble("speed") * 3.6, // Convert m/s to km/h
            humidity = main.getDouble("humidity"),
            pressure = main.getDouble("pressure")
        )
    }
}