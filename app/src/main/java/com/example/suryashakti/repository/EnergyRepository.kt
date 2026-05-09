package com.example.suryashakti.repository

import androidx.lifecycle.LiveData
import com.example.suryashakti.data.EnergyDao
import com.example.suryashakti.data.EnergyLog

class EnergyRepository(private val dao: EnergyDao, private val userId: String) {

    val allLogs: LiveData<List<EnergyLog>> = dao.getAllLogs(userId)
    val last30Days: LiveData<List<EnergyLog>> = dao.getLast30Days(userId)
    val totalSavings: LiveData<Float?> = dao.getTotalSavings(userId)
    val totalCo2Saved: LiveData<Float?> = dao.getTotalCo2Saved(userId)
    val totalDays: LiveData<Int> = dao.getTotalDays(userId)
    val totalExported: LiveData<Float?> = dao.getTotalExported(userId)

    suspend fun insertLog(log: EnergyLog) = dao.insertLog(log)
    suspend fun getLogByDate(date: String) = dao.getLogByDate(date, userId)
    suspend fun deleteLog(log: EnergyLog) = dao.deleteLog(log)
}