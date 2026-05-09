package com.example.suryashakti.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.suryashakti.databinding.FragmentInsightsBinding
import com.example.suryashakti.viewmodel.EnergyViewModel

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EnergyViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeData()
    }

    private fun observeData() {
        viewModel.totalCo2Saved.observe(viewLifecycleOwner) { co2 ->
            val co2Value = co2 ?: 0f
            binding.tvTotalCo2.text = "%.1f kg".format(co2Value)
            // 1 tree absorbs ~21kg CO2 per year
            val trees = (co2Value / 21f * 12).toInt()
            binding.tvTreesEquivalent.text = "$trees trees/month 🌳"
        }

        viewModel.totalSavings.observe(viewLifecycleOwner) { savings ->
            binding.tvInsightTotalSaved.text = "₹%.0f".format(savings ?: 0f)
        }

        viewModel.totalDays.observe(viewLifecycleOwner) { days ->
            binding.tvInsightDays.text = "$days"
            val avgDay = (viewModel.totalSavings.value ?: 0f) / (days.takeIf { it > 0 } ?: 1)
            binding.tvInsightAvgDay.text = "₹%.0f".format(avgDay)
        }

        viewModel.totalExported.observe(viewLifecycleOwner) { exported ->
            binding.tvInsightExported.text = "%.1f kWh".format(exported ?: 0f)
        }

        viewModel.last30Days.observe(viewLifecycleOwner) { logs ->
            if (logs.isEmpty()) return@observe
            val totalGen = logs.sumOf { it.generatedKwh.toDouble() }.toFloat()
            val totalCon = logs.sumOf { it.consumedKwh.toDouble() }.toFloat()
            binding.tvInsightGenerated.text = "%.1f kWh".format(totalGen)
            val coverage = if (totalCon > 0) ((totalGen / totalCon) * 100).toInt().coerceIn(0, 100) else 0
            binding.progressInsightCoverage.progress = coverage
            binding.tvInsightCoverage.text = "$coverage%"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}