package com.example.suryashakti.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.suryashakti.data.EnergyDatabase
import com.example.suryashakti.data.EnergyLog
import com.example.suryashakti.repository.EnergyRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EnergyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EnergyRepository
    val allLogs: LiveData<List<EnergyLog>>
    val last30Days: LiveData<List<EnergyLog>>
    val totalSavings: LiveData<Float?>
    val totalCo2Saved: LiveData<Float?>
    val totalDays: LiveData<Int>
    val totalExported: LiveData<Float?>

    // Get current user ID — guest gets "guest" as ID
    private val userId: String =
        FirebaseAuth.getInstance().currentUser?.uid ?: "guest"

    init {
        val dao = EnergyDatabase.getDatabase(application).energyDao()
        repository = EnergyRepository(dao, userId)
        allLogs = repository.allLogs
        last30Days = repository.last30Days
        totalSavings = repository.totalSavings
        totalCo2Saved = repository.totalCo2Saved
        totalDays = repository.totalDays
        totalExported = repository.totalExported
    }

    fun insertLog(log: EnergyLog) = viewModelScope.launch {
        // Always tag with current userId
        repository.insertLog(log.copy(userId = userId))
    }

    fun deleteLog(log: EnergyLog) = viewModelScope.launch {
        repository.deleteLog(log)
    }

    fun getTodayLog(date: String, callback: (EnergyLog?) -> Unit) {
        viewModelScope.launch {
            val log = repository.getLogByDate(date)
            callback(log)
        }
    }

    fun simulateGeneration(weather: String, panelKw: Float = 3.0f): Float {
        val peakHours = when (weather) {
            "Sunny"  -> (5.5f..7.5f).random()
            "Partly" -> (3.5f..5.5f).random()
            "Cloudy" -> (1.5f..3.5f).random()
            "Rainy"  -> (0.5f..1.5f).random()
            else -> 2.0f
        }
        return peakHours * panelKw / 3.0f
    }

    fun calculateSavings(log: EnergyLog): Float {
        val net = log.generatedKwh - log.consumedKwh
        return if (net > 0)
            (log.consumedKwh * log.pricePerUnit) + (net * log.exportRate)
        else log.generatedKwh * log.pricePerUnit
    }

    fun calculateExportEarnings(log: EnergyLog): Float {
        val net = log.generatedKwh - log.consumedKwh
        return if (net > 0) net * log.exportRate else 0f
    }

    fun calculateGridSaved(log: EnergyLog): Float {
        return minOf(log.generatedKwh, log.consumedKwh) * log.pricePerUnit
    }

    fun getIndependenceScore(log: EnergyLog): Int {
        if (log.consumedKwh == 0f) return 100
        return ((log.generatedKwh / log.consumedKwh) * 100).toInt().coerceIn(0, 100)
    }

    fun getPanelEfficiency(log: EnergyLog): Int {
        val theoretical = log.panelCapacityKw * 5.5f
        return ((log.generatedKwh / theoretical) * 100).toInt().coerceIn(0, 100)
    }

    fun calculateCo2Saved(generatedKwh: Float): Float = generatedKwh * 0.82f

    fun getScoreLabel(score: Int): String = when {
        score >= 100 -> "Solar Champion! \uD83C\uDF1F"
        score >= 80  -> "Energy Hero!"
        score >= 60  -> "Going Green!"
        score >= 40  -> "Making Progress"
        else         -> "Getting Started"
    }
}

fun ClosedFloatingPointRange<Float>.random(): Float {
    return start + (Math.random() * (endInclusive - start)).toFloat()
}