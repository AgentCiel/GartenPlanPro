package com.gartenplan.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a garden (the whole garden area)
 */
@Entity(tableName = "gardens")
data class GardenEntity(
    @PrimaryKey
    val id: String,
    
    val name: String,
    
    // Dimensions in cm
    val widthCm: Int,
    val heightCm: Int,
    
    // Location info
    val climateZone: String = "7a",     // German climate zone
    val postalCode: String? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Image (optional garden photo)
    val imageUri: String? = null,
    
    // Notes
    val notes: String? = null
)
