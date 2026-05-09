package com.example.suryashakti.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface EnergyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EnergyLog)

    @Query("SELECT * FROM energy_logs WHERE userId = :userId ORDER BY date DESC")
    fun getAllLogs(userId: String): LiveData<List<EnergyLog>>

    @Query("SELECT * FROM energy_logs WHERE userId = :userId ORDER BY date DESC LIMIT 30")
    fun getLast30Days(userId: String): LiveData<List<EnergyLog>>

    @Query("SELECT * FROM energy_logs WHERE date = :date AND userId = :userId LIMIT 1")
    suspend fun getLogByDate(date: String, userId: String): EnergyLog?

    @Delete
    suspend fun deleteLog(log: EnergyLog)

    @Query("SELECT COUNT(*) FROM energy_logs WHERE userId = :userId")
    fun getTotalDays(userId: String): LiveData<Int>

    @Query("SELECT SUM(co2SavedKg) FROM energy_logs WHERE userId = :userId")
    fun getTotalCo2Saved(userId: String): LiveData<Float?>

    @Query("""
        SELECT SUM(
            CASE WHEN generatedKwh > consumedKwh 
            THEN (consumedKwh * pricePerUnit) + ((generatedKwh - consumedKwh) * exportRate)
            ELSE generatedKwh * pricePerUnit END
        ) FROM energy_logs WHERE userId = :userId
    """)
    fun getTotalSavings(userId: String): LiveData<Float?>

    @Query("""SELECT SUM(
        CASE WHEN generatedKwh > consumedKwh 
        THEN (generatedKwh - consumedKwh) ELSE 0 END
    ) FROM energy_logs WHERE userId = :userId""")
    fun getTotalExported(userId: String): LiveData<Float?>
}