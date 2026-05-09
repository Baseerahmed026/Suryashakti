package com.example.suryashakti.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.suryashakti.LocationHelper
import com.example.suryashakti.R
import com.example.suryashakti.WeatherData
import com.example.suryashakti.WeatherManager
import com.example.suryashakti.databinding.FragmentDashboardBinding
import com.example.suryashakti.viewmodel.EnergyViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EnergyViewModel by activityViewModels()
    private lateinit var locationHelper: LocationHelper
    private lateinit var weatherManager: WeatherManager
    private var currentWeatherData: WeatherData? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            loadWeather()
        } else {
            setMockIrradiance()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationHelper = LocationHelper(requireContext())
        weatherManager = WeatherManager(requireContext())

        setDate()
        setupPieChart()
        observeData()
        checkLocationAndLoadWeather()
    }

    private fun checkLocationAndLoadWeather() {
        if (locationHelper.hasLocationPermission()) {
            loadWeather()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun loadWeather() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val location = locationHelper.getLocation()
                if (location != null) {
                    val weather = weatherManager.fetchWeather(location)
                    if (weather != null) {
                        currentWeatherData = weather
                        updateWeatherUI(weather)
                    } else {
                        setMockIrradiance()
                    }
                } else {
                    setMockIrradiance()
                }
            } catch (e: Exception) {
                setMockIrradiance()
            }
        }
    }

    private fun updateWeatherUI(weather: WeatherData) {
        binding.tvDate.text = SimpleDateFormat(
            "dd MMM yyyy", Locale.getDefault()).format(Date())

        // Irradiance
        binding.tvIrradiance.text = "${weather.irradiance} W/m²"
        binding.tvIrradianceStatus.text = when {
            weather.irradiance > 600 -> "Excellent ☀️"
            weather.irradiance > 400 -> "Very Good ⛅"
            weather.irradiance > 200 -> "Good"
            weather.irradiance > 100 -> "Moderate"
            else -> "Low"
        }
        binding.tvIrradianceStatus.setTextColor(
            when {
                weather.irradiance > 400 -> Color.parseColor("#00E676")
                weather.irradiance > 200 -> Color.parseColor("#FFD700")
                else -> Color.parseColor("#FF6B6B")
            }
        )

        // City + temperature
        binding.tvBestWindow.text =
            "📍 ${weather.cityName} • ${weather.temperature.toInt()}°C • " +
                    "Best window: ${weather.bestWindowStart}:00–${weather.bestWindowEnd}:00"

        // Weather badge
        binding.tvWeatherBadge.text = when (weather.condition) {
            "Sunny" -> "☀️ Sunny ${weather.temperature.toInt()}°C"
            "Partly" -> "⛅ Partly ${weather.temperature.toInt()}°C"
            "Cloudy" -> "☁️ Cloudy ${weather.temperature.toInt()}°C"
            "Rainy" -> "🌧️ Rainy ${weather.temperature.toInt()}°C"
            else -> weather.condition
        }
    }

    private fun setMockIrradiance() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val irradiance = when (hour) {
            in 10..16 -> (550..800).random()
            in 7..9, in 17..18 -> (200..500).random()
            else -> (0..100).random()
        }
        binding.tvIrradiance.text = "$irradiance W/m²"
        binding.tvIrradianceStatus.text = "Simulated"
        binding.tvBestWindow.text = "⚡ Best window: 10:00–16:00"
    }

    private fun setDate() {
        binding.tvDate.text =
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.parseColor("#1A1A1A"))
            holeRadius = 60f
            transparentCircleRadius = 63f
            setDrawCenterText(true)
            centerText = "Solar\nvs Grid"
            setCenterTextColor(Color.parseColor("#FFD700"))
            setCenterTextSize(11f)
            legend.isEnabled = false
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(9f)
            val entries = listOf(PieEntry(50f, "Solar"), PieEntry(50f, "Grid"))
            val dataSet = PieDataSet(entries, "").apply {
                colors = listOf(
                    Color.parseColor("#FFD700"),
                    Color.parseColor("#2A2A2A")
                )
                sliceSpace = 2f
            }
            data = PieData(dataSet).apply {
                setValueTextColor(Color.WHITE)
                setValueTextSize(9f)
            }
            invalidate()
        }
    }

    private fun observeData() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        viewModel.allLogs.observe(viewLifecycleOwner) { logs ->
            val todayLog = logs.firstOrNull { it.date == today }
            if (todayLog != null) {
                binding.tvGenerated.text = "%.1f".format(todayLog.generatedKwh)
                binding.tvConsumed.text = "%.1f".format(todayLog.consumedKwh)
                binding.tvWeatherBadge.text = when (todayLog.weatherCondition) {
                    "Sunny" -> "☀️ Sunny"
                    "Partly" -> "⛅ Partly"
                    "Cloudy" -> "☁️ Cloudy"
                    "Rainy" -> "🌧️ Rainy"
                    else -> todayLog.weatherCondition
                }

                val net = todayLog.generatedKwh - todayLog.consumedKwh
                val exportKwh = maxOf(0f, net)
                val exportEarned = viewModel.calculateExportEarnings(todayLog)
                val gridSaved = viewModel.calculateGridSaved(todayLog)
                val totalSavings = viewModel.calculateSavings(todayLog)
                val score = viewModel.getIndependenceScore(todayLog)
                val efficiency = viewModel.getPanelEfficiency(todayLog)

                binding.tvExport.text = "+%.2f kWh".format(exportKwh)
                binding.tvExportKwh.text = "%.2f kWh".format(exportKwh)
                binding.tvExportEarned.text = "₹%.2f earned".format(exportEarned)
                binding.tvGridSaved.text = "₹%.2f".format(gridSaved)
                binding.tvExportEarnedMain.text = "₹%.2f".format(exportEarned)
                binding.tvTotalToday.text = "₹%.2f".format(totalSavings)
                binding.tvScoreNumber.text = "$score"
                binding.tvScoreLabel.text = viewModel.getScoreLabel(score)
                binding.progressEfficiency.progress = efficiency
                binding.tvEfficiency.text = "$efficiency%"

                updatePieChart(todayLog.generatedKwh, todayLog.consumedKwh)
                updatePeakSuggestion(
                    todayLog.generatedKwh,
                    todayLog.consumedKwh,
                    todayLog.weatherCondition
                )
            }
        }

        viewModel.totalSavings.observe(viewLifecycleOwner) { total ->
            binding.tvTotalSavings.text = "₹%.0f".format(total ?: 0f)
        }

        viewModel.totalCo2Saved.observe(viewLifecycleOwner) { co2 ->
            binding.tvCo2.text = "%.1f kg".format(co2 ?: 0f)
        }

        viewModel.totalDays.observe(viewLifecycleOwner) { days ->
            binding.tvDays.text = "$days"
        }
    }

    private fun updatePieChart(generated: Float, consumed: Float) {
        val solar = minOf(generated, consumed)
        val grid = maxOf(0f, consumed - generated)
        val entries = if (grid == 0f)
            listOf(PieEntry(100f, "Solar ☀️"))
        else
            listOf(PieEntry(solar, "Solar ☀️"), PieEntry(grid, "Grid 🔌"))

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#FFD700"),
                Color.parseColor("#333333")
            )
            sliceSpace = 2f
        }
        binding.pieChart.data = PieData(dataSet).apply {
            setValueTextColor(Color.WHITE)
            setValueTextSize(9f)
        }
        binding.pieChart.invalidate()
    }

    private fun updatePeakSuggestion(generated: Float, consumed: Float, weather: String) {
        val weather_data = currentWeatherData
        binding.tvPeakSuggestion.text = when {
            weather_data != null && weather_data.irradiance > 500 && generated > consumed ->
                "🌞 PEAK SUN in ${weather_data.cityName}! ${weather_data.irradiance} W/m² — " +
                        "Run pump, washing machine or EV charger now for free solar energy!"
            weather == "Sunny" && generated > consumed ->
                "🌞 Over-generating! Great time to run pump, washing machine or EV charger!"
            weather == "Sunny" ->
                "☀️ Sunny day — shift heavy appliances to afternoon peak hours!"
            weather == "Partly" && generated > consumed ->
                "⛅ Partly cloudy but generating well — good time for moderate appliances."
            weather == "Cloudy" ->
                "☁️ Low generation today — conserve energy, avoid heavy appliances."
            weather == "Rainy" ->
                "🌧️ Rainy day — minimal solar generation. Rely on grid conservatively."
            generated > consumed ->
                "⚡ Over-generating! Extra units being exported to grid."
            else -> "✅ Balanced usage today. Keep it up!"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

fun ClosedRange<Int>.random() =
    (Random().nextInt(endInclusive - start + 1) + start)