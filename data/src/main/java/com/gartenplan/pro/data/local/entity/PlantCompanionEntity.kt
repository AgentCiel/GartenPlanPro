package com.gartenplan.pro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gartenplan.pro.core.constants.CompanionType

/**
 * Entity representing companion planting relationships between two plants
 * (Mischkultur - good/bad neighbors)
 */
@Entity(
    tableName = "plant_companions",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId1"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId2"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plantId1"]),
        Index(value = ["plantId2"]),
        Index(value = ["plantId1", "plantId2"], unique = true)
    ]
)
data class PlantCompanionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val plantId1: String,
    val plantId2: String,
    
    val relationship: CompanionType,
    
    // Reason for the relationship
    val reasonDE: String? = null,
    val reasonEN: String? = null
)
