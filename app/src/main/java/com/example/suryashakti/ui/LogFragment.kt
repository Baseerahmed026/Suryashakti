package com.example.suryashakti.ui
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.suryashakti.LocationHelper
import com.example.suryashakti.WeatherManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.suryashakti.data.EnergyLog
import com.example.suryashakti.databinding.FragmentLogBinding
import com.example.suryashakti.viewmodel.EnergyViewModel
import java.text.SimpleDateFormat
import java.util.*

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EnergyViewModel by activityViewModels()
    private var selectedWeather = "Sunny"
    private var existingLog: EnergyLog? = null
    private lateinit var today: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        binding.tvLogDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        checkExistingLog()
        setupWeatherButtons()
        setupSimulateButton()
        setupLivePreview()
        setupSaveButton()
    }

    private fun checkExistingLog() {
        viewModel.getTodayLog(today) { log ->
            existingLog = log
            if (log != null) {
                requireActivity().runOnUiThread {
                    binding.etGenerated.setText(log.generatedKwh.toString())
                    binding.etConsumed.setText(log.consumedKwh.toString())
                    binding.etPricePerUnit.setText(log.pricePerUnit.toString())
                    binding.etExportRate.setText(log.exportRate.toString())
                    selectedWeather = log.weatherCondition
                    updateWeatherUI(log.weatherCondition)
                    binding.tvUpdateBadge.text = "UPDATE"
                    binding.btnSaveLog.text = "🔄  UPDATE LOG"
                    Toast.makeText(requireContext(), "📝 Editing today's log", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupWeatherButtons() {
        binding.btnSunny.setOnClickListener { selectWeather("Sunny") }
        binding.btnPartly.setOnClickListener { selectWeather("Partly") }
        binding.btnCloudy.setOnClickListener { selectWeather("Cloudy") }
        binding.btnRainy.setOnClickListener { selectWeather("Rainy") }
    }

    private fun selectWeather(weather: String) {
        selectedWeather = weather
        updateWeatherUI(weather)
    }

    private fun updateWeatherUI(weather: String) {
        val gold = Color.parseColor("#FFD700")
        val dark = Color.parseColor("#1A1A1A")
        val gray = Color.parseColor("#888888")
        val black = Color.BLACK

        listOf(binding.btnSunny, binding.btnPartly, binding.btnCloudy, binding.btnRainy).forEach {
            it.backgroundTintList = android.content.res.ColorStateList.valueOf(dark)
            it.setTextColor(gray)
        }

        val selected = when (weather) {
            "Sunny" -> binding.btnSunny
            "Partly" -> binding.btnPartly
            "Cloudy" -> binding.btnCloudy
            "Rainy" -> binding.btnRainy
            else -> binding.btnSunny
        }
        selected.backgroundTintList = android.content.res.ColorStateList.valueOf(gold)
        selected.setTextColor(black)
    }

    private fun setupSimulateButton() {
        binding.btnSimulate.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val locationHelper = LocationHelper(requireContext())
                val weatherManager = WeatherManager(requireContext())
                val panelKw = requireContext()
                    .getSharedPreferences("surya_prefs", android.content.Context.MODE_PRIVATE)
                    .getFloat("panel_capacity", 3.0f)

                if (locationHelper.hasLocationPermission()) {
                    val location = locationHelper.getLocation()
                    if (location != null) {
                        val weather = weatherManager.fetchWeather(location)
                        if (weather != null) {
                            val simulated = weatherManager.simulateGeneration(weather, panelKw)
                            binding.etGenerated.setText("%.1f".format(simulated))
                            // Auto-set weather condition
                            selectWeather(weather.condition)
                            Toast.makeText(
                                requireContext(),
                                "🤖 Simulated from ${weather.cityName}: " +
                                        "${"%.1f".format(simulated)} kWh (${weather.temperature.toInt()}°C, " +
                                        "${weather.cloudPercent}% clouds)",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }
                    }
                }
                // Fallback
                val simulated = viewModel.simulateGeneration(selectedWeather, panelKw)
                binding.etGenerated.setText("%.1f".format(simulated))
                Toast.makeText(requireContext(),
                    "🤖 Simulated: ${"%.1f".format(simulated)} kWh",
                    Toast.LENGTH_SHORT).show()
            }
        }
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
        binding.etExportRate.addTextChangedListener(watcher)
    }

    private fun updatePreview() {
        val generated = binding.etGenerated.text.toString().toFloatOrNull() ?: 0f
        val consumed = binding.etConsumed.text.toString().toFloatOrNull() ?: 0f
        val price = binding.etPricePerUnit.text.toString().toFloatOrNull() ?: 8f
        val exportRate = binding.etExportRate.text.toString().toFloatOrNull() ?: 4f
        val net = generated - consumed
        val savings = if (net > 0)
            (consumed * price) + (net * exportRate)
        else generated * price

        binding.tvNetPreview.text = "₹ %.2f".format(savings)
        binding.tvNetKwh.text = when {
            net > 0 -> "Net: +%.1f kWh (Exporting to grid ⚡)".format(net)
            net < 0 -> "Net: %.1f kWh (Drawing from grid 🔌)".format(net)
            else -> "Net: 0.0 kWh (Perfectly balanced ✅)"
        }
        binding.tvNetPreview.setTextColor(
            if (net >= 0) Color.parseColor("#00E676")
            else Color.parseColor("#FF6B6B")
        )
    }

    private fun setupSaveButton() {
        binding.btnSaveLog.setOnClickListener {
            val generated = binding.etGenerated.text.toString().toFloatOrNull()
            val consumed = binding.etConsumed.text.toString().toFloatOrNull()
            val price = binding.etPricePerUnit.text.toString().toFloatOrNull() ?: 8f
            val exportRate = binding.etExportRate.text.toString().toFloatOrNull() ?: 4f

            if (generated == null || consumed == null) {
                Toast.makeText(requireContext(), "⚠️ Enter both generation and consumption", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (generated < 0 || consumed < 0) {
                Toast.makeText(requireContext(), "⚠️ Values cannot be negative", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (generated > 100 || consumed > 100) {
                Toast.makeText(requireContext(), "⚠️ Values above 100 kWh seem unrealistic", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val co2 = viewModel.calculateCo2Saved(generated)
            val log = EnergyLog(
                id = existingLog?.id ?: 0,
                date = today,
                generatedKwh = generated,
                consumedKwh = consumed,
                weatherCondition = selectedWeather,
                pricePerUnit = price,
                exportRate = exportRate,
                co2SavedKg = co2
            )
            viewModel.insertLog(log)
            val msg = if (existingLog != null) "✅ Log updated!" else "✅ Log saved!"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}