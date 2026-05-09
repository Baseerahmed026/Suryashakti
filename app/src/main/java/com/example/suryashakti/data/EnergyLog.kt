package com.example.suryashakti.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "energy_logs")
data class EnergyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",       // ← ADD THIS
    val date: String,
    val generatedKwh: Float,
    val consumedKwh: Float,
    val weatherCondition: String,
    val pricePerUnit: Float = 8.0f,
    val exportRate: Float = 4.0f,
    val panelCapacityKw: Float = 3.0f,
    val co2SavedKg: Float = 0f
)