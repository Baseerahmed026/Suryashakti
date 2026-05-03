package com.example.suryashakti.data



import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "energy_logs")
data class EnergyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val date: String,              // e.g. "2024-04-24"
    val generatedKwh: Float,       // Solar energy produced
    val consumedKwh: Float,        // Energy consumed from meter
    val weatherCondition: String,  // "Sunny" or "Cloudy"
    val pricePerUnit: Float = 8.0f // ₹ per kWh (default ₹8)
)