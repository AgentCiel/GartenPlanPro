package com.gartenplan.pro.data.local.converter

import androidx.room.TypeConverter
import com.gartenplan.pro.core.constants.*
import com.gartenplan.pro.data.local.entity.CompostStatus
import com.gartenplan.pro.data.local.entity.CultureType
import com.gartenplan.pro.data.local.entity.PlantingStatus

/**
 * Type converters for Room database
 * Converts enums to/from strings for storage
 */
class Converters {
    
    // PlantCategory
    @TypeConverter
    fun fromPlantCategory(value: PlantCategory): String = value.name
    
    @TypeConverter
    fun toPlantCategory(value: String): PlantCategory = PlantCategory.valueOf(value)
    
    // NutrientLevel
    @TypeConverter
    fun fromNutrientLevel(value: NutrientLevel): String = value.name
    
    @TypeConverter
    fun toNutrientLevel(value: String): NutrientLevel = NutrientLevel.valueOf(value)
    
    // WaterLevel
    @TypeConverter
    fun fromWaterLevel(value: WaterLevel): String = value.name
    
    @TypeConverter
    fun toWaterLevel(value: String): WaterLevel = WaterLevel.valueOf(value)
    
    // SunLevel
    @TypeConverter
    fun fromSunLevel(value: SunLevel): String = value.name
    
    @TypeConverter
    fun toSunLevel(value: String): SunLevel = SunLevel.valueOf(value)
    
    // CompanionType
    @TypeConverter
    fun fromCompanionType(value: CompanionType): String = value.name
    
    @TypeConverter
    fun toCompanionType(value: String): CompanionType = CompanionType.valueOf(value)
    
    // BedShape
    @TypeConverter
    fun fromBedShape(value: BedShape): String = value.name
    
    @TypeConverter
    fun toBedShape(value: String): BedShape = BedShape.valueOf(value)
    
    // TaskType
    @TypeConverter
    fun fromTaskType(value: TaskType): String = value.name
    
    @TypeConverter
    fun toTaskType(value: String): TaskType = TaskType.valueOf(value)
    
    // CultureType
    @TypeConverter
    fun fromCultureType(value: CultureType): String = value.name
    
    @TypeConverter
    fun toCultureType(value: String): CultureType = CultureType.valueOf(value)
    
    // PlantingStatus
    @TypeConverter
    fun fromPlantingStatus(value: PlantingStatus): String = value.name
    
    @TypeConverter
    fun toPlantingStatus(value: String): PlantingStatus = PlantingStatus.valueOf(value)
    
    // CompostMaterialType
    @TypeConverter
    fun fromCompostMaterialType(value: CompostMaterialType): String = value.name
    
    @TypeConverter
    fun toCompostMaterialType(value: String): CompostMaterialType = CompostMaterialType.valueOf(value)
    
    // CompostStatus
    @TypeConverter
    fun fromCompostStatus(value: CompostStatus): String = value.name
    
    @TypeConverter
    fun toCompostStatus(value: String): CompostStatus = CompostStatus.valueOf(value)
}
