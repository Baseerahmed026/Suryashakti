package com.example.suryashakti

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.suryashakti.data.EnergyLog
import com.example.suryashakti.databinding.ActivityLogEntryBinding
import com.example.suryashakti.viewmodel.EnergyViewModel
import java.text.SimpleDateFormat
import java.util.*

class LogEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogEntryBinding
    private val viewModel: EnergyViewModel by viewModels()
    private var selectedWeather = "Sunny"
    private var existingLog: EnergyLog? = null  // holds today's log if it exists
    private lateinit var today: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val displayDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        binding.tvLogDate.text = displayDate

        // Check if today already has a log
        checkExistingLog()

        setupWeatherButtons()
        setupSimulateSwitch()
        setupLivePreview()
        setupSaveButton()
    }

    private fun checkExistingLog() {
        viewModel.getTodayLog(today) { log ->
            existingLog = log
            if (log != null) {
                // Pre-fill with existing values
                runOnUiThread {
                    binding.etGenerated.setText(log.generatedKwh.toString())
                    binding.etConsumed.setText(log.consumedKwh.toString())
                    binding.etPricePerUnit.setText(log.pricePerUnit.toString())
                    selectedWeather = log.weatherCondition

                    // Update weather button UI
                    if (log.weatherCondition == "Cloudy") {
                        binding.btnCloudy.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFD700"))
                        binding.btnCloudy.setTextColor(android.graphics.Color.BLACK)
                        binding.btnSunny.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1A1A1A"))
                        binding.btnSunny.setTextColor(android.graphics.Color.parseColor("#FFD700"))
                    }

                    // Show update banner
                    Toast.makeText(
                        this,
                        "📝 Editing today's existing log",
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.btnSaveLog.text = "🔄 Update Log"
                }
            }
        }
    }

    private fun setupWeatherButtons() {
        binding.btnSunny.setOnClickListener {
            selectedWeather = "Sunny"
            binding.btnSunny.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFD700"))
            binding.btnSunny.setTextColor(android.graphics.Color.BLACK)
            binding.btnCloudy.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1A1A1A"))
            binding.btnCloudy.setTextColor(android.graphics.Color.parseColor("#FFD700"))
            if (binding.switchSimulate.isChecked) simulateGeneration()
        }

        binding.btnCloudy.setOnClickListener {
            selectedWeather = "Cloudy"
            binding.btnCloudy.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFD700"))
            binding.btnCloudy.setTextColor(android.graphics.Color.BLACK)
            binding.btnSunny.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1A1A1A"))
            binding.btnSunny.setTextColor(android.graphics.Color.parseColor("#FFD700"))
            if (binding.switchSimulate.isChecked) simulateGeneration()
        }
    }

    private fun setupSimulateSwitch() {
        binding.switchSimulate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                simulateGeneration()
                binding.etGenerated.isEnabled = false
            } else {
                binding.etGenerated.isEnabled = true
                binding.etGenerated.text?.clear()
            }
        }
    }

    private fun simulateGeneration() {
        val simulated = viewModel.simulateGeneration(selectedWeather)
        binding.etGenerated.setText("%.1f".format(simulated))
    }

    private fun setupLivePreview() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updatePreview() }
        }
        binding.etGenerated.addTextChangedListener(watcher)
        binding.etConsumed.addTextChangedListener(watcher)
        binding.etPricePerUnit.addTextChangedListener(watcher)
    }

    private fun updatePreview() {
        val generated = binding.etGenerated.text.toString().toFloatOrNull() ?: 0f
        val consumed = binding.etConsumed.text.toString().toFloatOrNull() ?: 0f
        val price = binding.etPricePerUnit.text.toString().toFloatOrNull() ?: 8f
        val net = generated - consumed
        val savings = if (net > 0) net * price else 0f

        binding.tvNetPreview.text = "₹ %.2f".format(savings)
        binding.tvNetKwh.text = when {
            net > 0 -> "Net: +%.1f kWh (Exporting to grid ⚡)".format(net)
            net < 0 -> "Net: %.1f kWh (Drawing from grid 🔌)".format(net)
            else -> "Net: 0.0 kWh (Perfectly balanced ✅)"
        }
        binding.tvNetPreview.setTextColor(
            if (net >= 0) android.graphics.Color.parseColor("#00FF88")
            else android.graphics.Color.parseColor("#FF6B6B")
        )
    }

    private fun setupSaveButton() {
        binding.btnSaveLog.setOnClickListener {
            val generated = binding.etGenerated.text.toString().toFloatOrNull()
            val consumed = binding.etConsumed.text.toString().toFloatOrNull()
            val price = binding.etPricePerUnit.text.toString().toFloatOrNull() ?: 8f

            // Rule 1: Both fields required
            if (generated == null || consumed == null) {
                Toast.makeText(this, "⚠️ Please enter both generation and consumption values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Rule 2: No negative values
            if (generated < 0 || consumed < 0) {
                Toast.makeText(this, "⚠️ Values cannot be negative", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Rule 3: Sanity check — unrealistic values
            if (generated > 50 || consumed > 50) {
                Toast.makeText(this, "⚠️ Values above 50 kWh seem too high. Please check.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Rule 4: Price sanity
            if (price <= 0 || price > 100) {
                Toast.makeText(this, "⚠️ Price per unit should be between ₹1 and ₹100", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Build log — reuse existing ID if updating
            val log = EnergyLog(
                id = existingLog?.id ?: 0,
                date = today,
                generatedKwh = generated,
                consumedKwh = consumed,
                weatherCondition = selectedWeather,
                pricePerUnit = price
            )

            viewModel.insertLog(log)

            val msg = if (existingLog != null) "✅ Log updated!" else "✅ Log saved!"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}