package com.example.suryashakti.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.suryashakti.data.EnergyDatabase
import com.example.suryashakti.data.EnergyLog
import com.example.suryashakti.repository.EnergyRepository
import kotlinx.coroutines.launch

class EnergyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EnergyRepository
    val allLogs: LiveData<List<EnergyLog>>
    val last30Days: LiveData<List<EnergyLog>>
    val totalSavings: LiveData<Float?>

    init {
        val dao = EnergyDatabase.getDatabase(application).energyDao()
        repository = EnergyRepository(dao)
        allLogs = repository.allLogs
        last30Days = repository.last30Days
        totalSavings = repository.totalSavings
    }

    fun insertLog(log: EnergyLog) = viewModelScope.launch {
        repository.insertLog(log)
    }

    fun deleteLog(log: EnergyLog) = viewModelScope.launch {
        repository.deleteLog(log)
    }

    fun simulateGeneration(weather: String): Float {
        return when (weather) {
            "Sunny" -> (4.0f..8.0f).random()
            "Cloudy" -> (1.0f..3.5f).random()
            else -> 2.0f
        }
    }

    fun calculateSavings(log: EnergyLog): Float {
        val net = log.generatedKwh - log.consumedKwh
        return if (net > 0) net * log.pricePerUnit else 0f
    }

    fun getIndependenceScore(log: EnergyLog): Int {
        if (log.consumedKwh == 0f) return 100
        val score = (log.generatedKwh / log.consumedKwh) * 100
        return score.toInt().coerceIn(0, 100)
    }

    // ← NOW INSIDE the class
    fun getTodayLog(date: String, callback: (EnergyLog?) -> Unit) {
        viewModelScope.launch {
            val log = repository.getLogByDate(date)
            callback(log)
        }
    }

} // ← END of class

// Extension function stays OUTSIDE the class
fun ClosedFloatingPointRange<Float>.random(): Float {
    return start + (Math.random() * (endInclusive - start)).toFloat()
}