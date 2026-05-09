package com.example.suryashakti

import android.content.Context
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Calendar

data class WeatherData(
    val cityName: String,
    val temperature: Float,
    val condition: String,       // Sunny, Partly, Cloudy, Rainy
    val cloudPercent: Int,
    val humidity: Int,
    val description: String,
    val irradiance: Int,         // calculated W/m²
    val bestWindowStart: Int,    // hour
    val bestWindowEnd: Int       // hour
)

class WeatherManager(private val context: Context) {

    private val API_KEY = "1090457e6e566c17a9817a1fb85dfa5b"

    suspend fun fetchWeather(location: Location): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.openweathermap.org/data/2.5/weather" +
                        "?lat=${location.latitude}" +
                        "&lon=${location.longitude}" +
                        "&appid=$API_KEY" +
                        "&units=metric"

                val response = URL(url).readText()
                val json = JSONObject(response)

                val main = json.getJSONObject("main")
                val weather = json.getJSONArray("weather").getJSONObject(0)
                val clouds = json.getJSONObject("clouds")
                val cityName = json.getString("name")

                val temp = main.getDouble("temp").toFloat()
                val humidity = main.getInt("humidity")
                val cloudPercent = clouds.getInt("all")
                val weatherId = weather.getInt("id")
                val description = weather.getString("description")

                // Map OpenWeatherMap ID to our condition
                val condition = when {
                    weatherId in 200..299 -> "Rainy"   // Thunderstorm
                    weatherId in 300..399 -> "Rainy"   // Drizzle
                    weatherId in 500..599 -> "Rainy"   // Rain
                    weatherId in 600..699 -> "Cloudy"  // Snow
                    weatherId in 700..799 -> "Cloudy"  // Atmosphere
                    weatherId == 800 -> "Sunny"         // Clear
                    weatherId == 801 -> "Partly"        // Few clouds
                    weatherId == 802 -> "Partly"        // Scattered clouds
                    weatherId in 803..804 -> "Cloudy"  // Broken/overcast
                    else -> "Partly"
                }

                // Calculate irradiance from cloud cover + time
                val irradiance = calculateIrradiance(cloudPercent)

                // Best window based on condition
                val (windowStart, windowEnd) = getBestWindow(condition)

                WeatherData(
                    cityName = cityName,
                    temperature = temp,
                    condition = condition,
                    cloudPercent = cloudPercent,
                    humidity = humidity,
                    description = description,
                    irradiance = irradiance,
                    bestWindowStart = windowStart,
                    bestWindowEnd = windowEnd
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun calculateIrradiance(cloudPercent: Int): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Base irradiance by time of day
        val baseIrradiance = when (hour) {
            in 6..7 -> 150
            in 8..9 -> 350
            in 10..14 -> 900
            in 15..16 -> 600
            in 17..18 -> 300
            else -> 0
        }

        // Reduce by cloud cover
        val cloudFactor = 1.0f - (cloudPercent / 100f * 0.85f)
        return (baseIrradiance * cloudFactor).toInt()
    }

    private fun getBestWindow(condition: String): Pair<Int, Int> {
        return when (condition) {
            "Sunny" -> Pair(9, 16)
            "Partly" -> Pair(10, 15)
            "Cloudy" -> Pair(11, 14)
            else -> Pair(12, 13)
        }
    }

    // Smart simulation based on real weather
    fun simulateGeneration(weatherData: WeatherData, panelKw: Float): Float {
        val peakSunHours = when {
            weatherData.cloudPercent < 10 -> 6.5f   // Clear
            weatherData.cloudPercent < 30 -> 5.5f   // Mostly clear
            weatherData.cloudPercent < 60 -> 3.5f   // Partly cloudy
            weatherData.cloudPercent < 80 -> 2.0f   // Mostly cloudy
            else -> 0.8f                             // Overcast/Rainy
        }

        // Adjust for temperature (panels lose ~0.5% per °C above 25°C)
        val tempFactor = if (weatherData.temperature > 25)
            1.0f - ((weatherData.temperature - 25) * 0.005f)
        else 1.0f

        return (peakSunHours * panelKw * tempFactor).coerceIn(0.1f, panelKw * 8f)
    }
}