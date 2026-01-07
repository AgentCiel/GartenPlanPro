package com.gartenplan.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gartenplan.pro.core.constants.NutrientLevel
import com.gartenplan.pro.core.constants.PlantCategory
import com.gartenplan.pro.core.constants.SunLevel
import com.gartenplan.pro.core.constants.WaterLevel

/**
 * Plant entity representing a plant species in the database
 */
@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey
    val id: String,
    
    // Names
    val nameDE: String,
    val nameEN: String,
    val latinName: String? = null,
    
    // Category
    val category: PlantCategory,
    
    // Timing (months 1-12)
    val sowIndoorStart: Int? = null,    // Vorziehen Start
    val sowIndoorEnd: Int? = null,      // Vorziehen Ende
    val sowOutdoorStart: Int? = null,   // Direktsaat Start
    val sowOutdoorEnd: Int? = null,     // Direktsaat Ende
    val harvestStart: Int,              // Ernte Start
    val harvestEnd: Int,                // Ernte Ende
    
    // Growing time
    val daysToGermination: Int? = null, // Tage bis Keimung
    val daysToHarvest: Int? = null,     // Tage bis Ernte
    
    // Spacing (in cm)
    val spacingInRowCm: Int,            // Abstand in der Reihe
    val spacingBetweenRowsCm: Int,      // Reihenabstand
    val plantDepthCm: Int? = null,      // Pflanztiefe
    
    // Requirements
    val nutrientDemand: NutrientLevel,
    val waterDemand: WaterLevel,
    val sunRequirement: SunLevel,
    
    // Additional info
    val description: String? = null,
    val careTips: String? = null,
    val diseases: String? = null,       // Comma-separated
    val pests: String? = null,          // Comma-separated
    
    // Image
    val imageUrl: String? = null,
    
    // Flags
    val isFavorite: Boolean = false,
    val isProOnly: Boolean = false
)
