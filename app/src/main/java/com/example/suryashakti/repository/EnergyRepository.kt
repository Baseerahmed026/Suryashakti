package com.example.suryashakti.repository



import androidx.lifecycle.LiveData
import com.example.suryashakti.data.EnergyDao
import com.example.suryashakti.data.EnergyLog

class EnergyRepository(private val dao: EnergyDao) {

    val allLogs: LiveData<List<EnergyLog>> = dao.getAllLogs()
    val last30Days: LiveData<List<EnergyLog>> = dao.getLast30Days()
    val totalSavings: LiveData<Float?> = dao.getTotalSavings()

    suspend fun insertLog(log: EnergyLog) {
        dao.insertLog(log)
    }

    suspend fun getLogByDate(date: String): EnergyLog? {
        return dao.getLogByDate(date)
    }

    suspend fun deleteLog(log: EnergyLog) {
        dao.deleteLog(log)
    }
}