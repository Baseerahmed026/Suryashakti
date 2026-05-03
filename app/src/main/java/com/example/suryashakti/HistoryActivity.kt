package com.example.suryashakti

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.suryashakti.data.EnergyLog
import com.example.suryashakti.databinding.ActivityHistoryBinding
import com.example.suryashakti.databinding.ItemLogBinding
import com.example.suryashakti.viewmodel.EnergyViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: EnergyViewModel by viewModels()
    private lateinit var adapter: LogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = LogAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun observeData() {
        viewModel.last30Days.observe(this) { logs ->
            adapter.submitList(logs)
            setupBarChart(logs)
        }

        viewModel.totalSavings.observe(this) { total ->
            binding.tvHistoryTotalSavings.text = "₹ %.2f".format(total ?: 0f)
        }
    }

    private fun setupBarChart(logs: List<EnergyLog>) {
        if (logs.isEmpty()) return

        val reversed = logs.reversed()
        val genEntries = reversed.mapIndexed { i, log -> BarEntry(i.toFloat(), log.generatedKwh) }
        val conEntries = reversed.mapIndexed { i, log -> BarEntry(i.toFloat(), log.consumedKwh) }
        val labels = reversed.map { it.date.substring(5) } // MM-DD

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

    // --- Adapter ---
    inner class LogAdapter : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

        private var logs = listOf<EnergyLog>()

        fun submitList(newLogs: List<EnergyLog>) {
            logs = newLogs
            notifyDataSetChanged()
        }

        inner class LogViewHolder(val binding: ItemLogBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
            val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return LogViewHolder(binding)
        }

        override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
            val log = logs[position]
            holder.binding.apply {
                tvWeatherIcon.text = if (log.weatherCondition == "Sunny") "☀️" else "☁️"
                tvLogItemDate.text = log.date
                tvLogItemStats.text = "Gen: %.1f | Con: %.1f kWh".format(log.generatedKwh, log.consumedKwh)
                val savings = viewModel.calculateSavings(log)
                tvLogItemSavings.text = "₹%.2f".format(savings)
            }
        }

        override fun getItemCount() = logs.size
    }
}