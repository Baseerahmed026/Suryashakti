package com.example.suryashakti.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EnergyLog::class], version = 3, exportSchema = false)
abstract class EnergyDatabase : RoomDatabase() {

    abstract fun energyDao(): EnergyDao

    companion object {
        @Volatile
        private var INSTANCE: EnergyDatabase? = null

        fun getDatabase(context: Context): EnergyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EnergyDatabase::class.java,
                    "energy_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}