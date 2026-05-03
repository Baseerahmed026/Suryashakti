package com.example.suryashakti.data


import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface EnergyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EnergyLog)

    @Query("SELECT * FROM energy_logs ORDER BY date DESC")
    fun getAllLogs(): LiveData<List<EnergyLog>>

    @Query("SELECT * FROM energy_logs ORDER BY date DESC LIMIT 30")
    fun getLast30Days(): LiveData<List<EnergyLog>>

    @Query("SELECT * FROM energy_logs WHERE date = :date LIMIT 1")
    suspend fun getLogByDate(date: String): EnergyLog?

    @Delete
    suspend fun deleteLog(log: EnergyLog)

    @Query("SELECT SUM((generatedKwh - consumedKwh) * pricePerUnit) FROM energy_logs WHERE generatedKwh > consumedKwh")
    fun getTotalSavings(): LiveData<Float?>
}