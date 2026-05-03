package com.example.suryashakti

import java.util.Calendar
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.suryashakti.databinding.ActivityMainBinding
import com.example.suryashakti.viewmodel.EnergyViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: EnergyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTodayDate()
        setupPieChart()
        observeData()
        setupButtons()
    }

    private fun setTodayDate() {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.tvDate.text = sdf.format(Date())
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.parseColor("#000000"))
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Solar\nvs Grid"
            setCenterTextColor(Color.parseColor("#FFD700"))
            setCenterTextSize(12f)
            legend.isEnabled = false
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(10f)

            // Default empty state
            val entries = listOf(
                PieEntry(50f, "Solar"),
                PieEntry(50f, "Grid")
            )
            val dataSet = PieDataSet(entries, "").apply {
                colors = listOf(
                    Color.parseColor("#FFD700"),
                    Color.parseColor("#333333")
                )
                sliceSpace = 2f
            }
            data = PieData(dataSet).apply {
                setValueTextColor(Color.WHITE)
                setValueTextSize(10f)
            }
            invalidate()
        }
    }

    private fun observeData() {
        // Watch latest log for today's stats
        viewModel.allLogs.observe(this) { logs ->
            if (logs.isNotEmpty()) {
                val latest = logs.first()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                if (latest.date == today) {
                    // Update stats
                    binding.tvGenerated.text = "%.1f kWh".format(latest.generatedKwh)
                    binding.tvConsumed.text = "%.1f kWh".format(latest.consumedKwh)
                    binding.tvWeather.text = latest.weatherCondition

                    // Savings
                    val savings = viewModel.calculateSavings(latest)
                    binding.tvSavings.text = "₹ %.2f".format(savings)

                    // Independence score
                    val score = viewModel.getIndependenceScore(latest)
                    binding.tvScoreNumber.text = "$score"

                    // Update pie chart
                    updatePieChart(latest.generatedKwh, latest.consumedKwh)

                    // Peak suggestion
                    updatePeakSuggestion(latest.generatedKwh, latest.consumedKwh, latest.weatherCondition)
                } else {
                    binding.tvPeakSuggestion.text = "💡 No entry for today yet — tap Log to add!"
                }
            } else {
                binding.tvPeakSuggestion.text = "💡 Welcome! Log your first energy entry below."
            }
        }

        // Total 30-day savings
        viewModel.totalSavings.observe(this) { total ->
            binding.tvTotalSavings.text = "₹ %.2f".format(total ?: 0f)
        }
        // Streak counter
        viewModel.allLogs.observe(this) { logs ->
            var streak = 0
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            for (i in 0 until logs.size) {
                val expected = sdf.format(calendar.time)
                if (logs.getOrNull(i)?.date == expected) {
                    streak++
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                } else break
            }
            binding.tvStreak.text = "$streak 🔥"
        }
    }

    private fun updatePieChart(generated: Float, consumed: Float) {
        val solar = minOf(generated, consumed)
        val grid = maxOf(0f, consumed - generated)

        val entries = if (grid == 0f) {
            listOf(PieEntry(100f, "Solar ☀️"))
        } else {
            listOf(
                PieEntry(solar, "Solar ☀️"),
                PieEntry(grid, "Grid 🔌")
            )
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#FFD700"),
                Color.parseColor("#333333")
            )
            sliceSpace = 2f
        }

        binding.pieChart.data = PieData(dataSet).apply {
            setValueTextColor(Color.WHITE)
            setValueTextSize(10f)
        }
        binding.pieChart.invalidate()
    }

    private fun updatePeakSuggestion(generated: Float, consumed: Float, weather: String) {
        binding.tvPeakSuggestion.text = when {
            weather == "Sunny" && generated > consumed ->
                "🌞 High Sun! Over-generating — great time to run pump, washing machine!"
            weather == "Sunny" && generated <= consumed ->
                "☀️ Sunny day — shift heavy appliances to afternoon peak hours!"
            weather == "Cloudy" && generated < 2f ->
                "☁️ Low generation today — conserve energy, avoid heavy appliances."
            generated > consumed ->
                "⚡ You're over-generating! Extra units exported to grid."
            else ->
                "✅ Balanced usage today. Keep it up!"//balanced
        }
    }

    private fun setupButtons() {
        binding.btnLogEnergy.setOnClickListener {
            startActivity(Intent(this, LogEntryActivity::class.java))
        }
        binding.btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }
}