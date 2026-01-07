package com.gartenplan.pro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gartenplan.pro.core.constants.BedShape

/**
 * Entity representing a bed within a garden
 */
@Entity(
    tableName = "beds",
    foreignKeys = [
        ForeignKey(
            entity = GardenEntity::class,
            parentColumns = ["id"],
            childColumns = ["gardenId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gardenId"])]
)
data class BedEntity(
    @PrimaryKey
    val id: String,
    
    val gardenId: String,
    
    val name: String,
    
    // Position in the garden (in cm from top-left)
    val positionX: Int,
    val positionY: Int,
    
    // Dimensions in cm
    val widthCm: Int,
    val heightCm: Int,
    
    // Shape
    val shape: BedShape = BedShape.RECTANGLE,
    
    // For L-shaped beds (secondary dimensions)
    val secondaryWidthCm: Int? = null,
    val secondaryHeightCm: Int? = null,
    
    // Raised bed height (0 for ground-level)
    val raisedHeightCm: Int = 0,
    
    // Is this a path/walkway?
    val isPath: Boolean = false,
    
    // Visual customization
    val colorHex: String = "#8D6E63",   // Brown default
    
    // Soil info
    val soilType: String? = null,
    
    // Notes
    val notes: String? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
