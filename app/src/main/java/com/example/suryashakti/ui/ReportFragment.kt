package com.example.suryashakti.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.suryashakti.data.EnergyLog
import com.example.suryashakti.databinding.FragmentReportBinding
import com.example.suryashakti.databinding.ItemLogBinding
import com.example.suryashakti.viewmodel.EnergyViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EnergyViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        observeData()
    }

    private fun observeData() {
        viewModel.last30Days.observe(viewLifecycleOwner) { logs ->
            if (logs.isEmpty()) return@observe
            setupBarChart(logs)
            setupRecycler(logs)

            val totalGen = logs.sumOf { it.generatedKwh.toDouble() }.toFloat()
            val totalCon = logs.sumOf { it.consumedKwh.toDouble() }.toFloat()
            val totalExp = logs.sumOf { maxOf(0f, it.generatedKwh - it.consumedKwh).toDouble() }.toFloat()
            val avgScore = logs.map { viewModel.getIndependenceScore(it) }.average().toInt()
            val totalGridSaved = logs.sumOf { viewModel.calculateGridSaved(it).toDouble() }.toFloat()
            val totalExportEarned = logs.sumOf { viewModel.calculateExportEarnings(it).toDouble() }.toFloat()
            val totalBenefit = totalGridSaved + totalExportEarned
            val coverage = if (totalCon > 0) ((totalGen / totalCon) * 100).toInt().coerceIn(0, 100) else 0

            binding.tvTotalGenerated.text = "%.1f".format(totalGen)
            binding.tvTotalConsumed.text = "%.1f".format(totalCon)
            binding.tvTotalExported.text = "%.1f".format(totalExp)
            binding.tvTotalGridSaved.text = "₹%.0f".format(totalGridSaved)
            binding.tvTotalExportEarned.text = "₹%.0f".format(totalExportEarned)
            binding.tvTotalBenefit.text = "₹%.0f".format(totalBenefit)
            binding.tvAvgScore.text = "$avgScore%"
            binding.progressAvgScore.progress = avgScore
            binding.tvLoggedDays.text = "${logs.size}/30 days"
            binding.tvChampionLabel.text = viewModel.getScoreLabel(avgScore)
            binding.progressSolarCoverage.progress = coverage
            binding.tvSolarCoverage.text = "$coverage%"
        }
    }

    private fun setupBarChart(logs: List<EnergyLog>) {
        val reversed = logs.reversed()
        val genEntries = reversed.mapIndexed { i, l -> BarEntry(i.toFloat(), l.generatedKwh) }
        val conEntries = reversed.mapIndexed { i, l -> BarEntry(i.toFloat(), l.consumedKwh) }
        val labels = reversed.map { it.date.substring(5) }

        val genSet = BarDataSet(genEntries, "Generated").apply {
            color = Color.parseColor("#FFD700")
            setDrawValues(false)
        }
        val conSet = BarDataSet(conEntries, "Consumed").apply {
            color = Color.parseColor("#FF6B6B")
            setDrawValues(false)
        }

        binding.barChart.apply {
            data = BarData(genSet, conSet).apply { barWidth = 0.35f }
            groupBars(0f, 0.1f, 0.05f)
            description.isEnabled = false
            setFitBars(true)
            legend.textColor = Color.WHITE
            axisLeft.textColor = Color.WHITE
            axisRight.isEnabled = false
            xAxis.apply {
                textColor = Color.WHITE
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
            }
            setBackgroundColor(Color.parseColor("#1A1A1A"))
            invalidate()
        }
    }

    private fun setupRecycler(logs: List<EnergyLog>) {
        binding.recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(val b: ItemLogBinding) : RecyclerView.ViewHolder(b.root)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                VH(ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

            override fun getItemCount() = logs.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val log = logs[position]
                val b = (holder as VH).b
                b.tvWeatherIcon.text = when (log.weatherCondition) {
                    "Sunny" -> "☀️"
                    "Partly" -> "⛅"
                    "Rainy" -> "🌧️"
                    else -> "☁️"
                }
                b.tvLogItemDate.text = log.date
                b.tvLogItemStats.text = "Gen: %.1f | Con: %.1f kWh".format(log.generatedKwh, log.consumedKwh)
                b.tvLogItemSavings.text = "₹%.2f".format(viewModel.calculateSavings(log))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}