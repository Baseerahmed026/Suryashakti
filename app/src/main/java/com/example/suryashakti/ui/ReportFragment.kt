package com.example.suryashakti.ui

import android.content.ContentValues
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EnergyViewModel by activityViewModels()
    private var currentLogs: List<EnergyLog> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        observeData()

        binding.btnExportCsv.setOnClickListener {
            exportToCsv(currentLogs)
        }
    }

    private fun observeData() {
        viewModel.last30Days.observe(viewLifecycleOwner) { logs ->
            if (logs.isEmpty()) return@observe
            currentLogs = logs
            setupBarChart(logs)
            setupRecycler(logs)

            val totalGen = logs.sumOf { it.generatedKwh.toDouble() }.toFloat()
            val totalCon = logs.sumOf { it.consumedKwh.toDouble() }.toFloat()
            val totalExp = logs.sumOf {
                maxOf(0f, it.generatedKwh - it.consumedKwh).toDouble()
            }.toFloat()
            val avgScore = logs.map { viewModel.getIndependenceScore(it) }.average().toInt()
            val totalGridSaved = logs.sumOf {
                viewModel.calculateGridSaved(it).toDouble()
            }.toFloat()
            val totalExportEarned = logs.sumOf {
                viewModel.calculateExportEarnings(it).toDouble()
            }.toFloat()
            val totalBenefit = totalGridSaved + totalExportEarned
            val coverage = if (totalCon > 0)
                ((totalGen / totalCon) * 100).toInt().coerceIn(0, 100)
            else 0

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
        val genEntries = reversed.mapIndexed { i, l ->
            BarEntry(i.toFloat(), l.generatedKwh)
        }
        val conEntries = reversed.mapIndexed { i, l ->
            BarEntry(i.toFloat(), l.consumedKwh)
        }
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
                b.tvLogItemStats.text =
                    "Gen: %.1f | Con: %.1f kWh".format(log.generatedKwh, log.consumedKwh)
                b.tvLogItemSavings.text = "₹%.2f".format(viewModel.calculateSavings(log))
            }
        }
    }

    private fun exportToCsv(logs: List<EnergyLog>) {
        if (logs.isEmpty()) {
            Toast.makeText(requireContext(), "No logs to export", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "SuryaShakti_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        }.csv"

        val csvContent = buildString {
            appendLine(
                "Date,Weather,Generated(kWh),Consumed(kWh)," +
                        "Net(kWh),Grid Saved(Rs),Export Earned(Rs)," +
                        "Total Savings(Rs),CO2 Saved(kg)"
            )
            logs.forEach { log ->
                val net = log.generatedKwh - log.consumedKwh
                val gridSaved = viewModel.calculateGridSaved(log)
                val exportEarned = viewModel.calculateExportEarnings(log)
                val totalSavings = viewModel.calculateSavings(log)
                appendLine(
                    "${log.date}," +
                            "${log.weatherCondition}," +
                            "${"%.2f".format(log.generatedKwh)}," +
                            "${"%.2f".format(log.consumedKwh)}," +
                            "${"%.2f".format(net)}," +
                            "${"%.2f".format(gridSaved)}," +
                            "${"%.2f".format(exportEarned)}," +
                            "${"%.2f".format(totalSavings)}," +
                            "${"%.2f".format(log.co2SavedKg)}"
                )
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                    )
                }
                val uri = requireContext().contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let {
                    requireContext().contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(csvContent.toByteArray())
                    }
                    Toast.makeText(
                        requireContext(),
                        "✅ Exported! Check Downloads/$fileName",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                val file = java.io.File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ),
                    fileName
                )
                file.writeText(csvContent)
                Toast.makeText(
                    requireContext(),
                    "✅ Exported to Downloads/$fileName",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: IOException) {
            Toast.makeText(
                requireContext(),
                "❌ Export failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}