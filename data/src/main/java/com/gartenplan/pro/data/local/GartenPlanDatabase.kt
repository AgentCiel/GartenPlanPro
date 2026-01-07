package com.gartenplan.pro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gartenplan.pro.data.local.converter.Converters
import com.gartenplan.pro.data.local.dao.*
import com.gartenplan.pro.data.local.entity.*

/**
 * Main Room Database for GartenPlan Pro
 */
@Database(
    entities = [
        PlantEntity::class,
        PlantCompanionEntity::class,
        GardenEntity::class,
        BedEntity::class,
        PlantPlacementEntity::class,
        TaskEntity::class,
        CompostEntity::class,
        CompostEntryEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GartenPlanDatabase : RoomDatabase() {
    
    abstract fun plantDao(): PlantDao
    abstract fun gardenDao(): GardenDao
    abstract fun taskDao(): TaskDao
    abstract fun compostDao(): CompostDao
    
    companion object {
        const val DATABASE_NAME = "gartenplan_database"
    }
}
