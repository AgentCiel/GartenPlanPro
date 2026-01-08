package com.gartenplan.pro.feature.garden.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import java.util.UUID

// ==================== GEOMETRY ====================

data class CanvasPoint(val x: Float, val y: Float) {
    fun toOffset() = Offset(x, y)
    fun distanceTo(other: CanvasPoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    companion object {
        fun fromOffset(offset: Offset) = CanvasPoint(offset.x, offset.y)
    }
}

data class CanvasPolygon(
    val points: List<CanvasPoint>
) {
    val bounds: Rect get() {
        if (points.isEmpty()) return Rect.Zero
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        return Rect(minX, minY, maxX, maxY)
    }
    
    val center: CanvasPoint get() {
        if (points.isEmpty()) return CanvasPoint(0f, 0f)
        return CanvasPoint(
            points.map { it.x }.average().toFloat(),
            points.map { it.y }.average().toFloat()
        )
    }
    
    fun contains(point: CanvasPoint): Boolean {
        // Ray casting algorithm
        if (points.size < 3) return false
        var inside = false
        var j = points.size - 1
        for (i in points.indices) {
            if ((points[i].y > point.y) != (points[j].y > point.y) &&
                point.x < (points[j].x - points[i].x) * (point.y - points[i].y) / 
                    (points[j].y - points[i].y) + points[i].x) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
    
    // Smooth polygon using Chaikin's algorithm
    fun smoothed(iterations: Int = 2): CanvasPolygon {
        var current = points
        repeat(iterations) {
            val smoothed = mutableListOf<CanvasPoint>()
            for (i in current.indices) {
                val p0 = current[i]
                val p1 = current[(i + 1) % current.size]
                smoothed.add(CanvasPoint(
                    0.75f * p0.x + 0.25f * p1.x,
                    0.75f * p0.y + 0.25f * p1.y
                ))
                smoothed.add(CanvasPoint(
                    0.25f * p0.x + 0.75f * p1.x,
                    0.25f * p0.y + 0.75f * p1.y
                ))
            }
            current = smoothed
        }
        return CanvasPolygon(current)
    }
    
    // Simplify polygon (reduce points)
    fun simplified(tolerance: Float = 5f): CanvasPolygon {
        if (points.size <= 4) return this
        val simplified = mutableListOf(points.first())
        for (i in 1 until points.size) {
            if (points[i].distanceTo(simplified.last()) > tolerance) {
                simplified.add(points[i])
            }
        }
        return CanvasPolygon(simplified)
    }
}

// ==================== BED MODEL ====================

data class CanvasBed(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val emoji: String? = null,
    val polygon: CanvasPolygon,
    val colorHex: String = BedColors.random(),
    val plantAreas: List<PlantArea> = emptyList()
) {
    val bounds: Rect get() = polygon.bounds
    val center: CanvasPoint get() = polygon.center
    
    fun contains(point: CanvasPoint): Boolean = polygon.contains(point)
    
    fun displayName(): String = buildString {
        emoji?.let { append(it).append(" ") }
        append(name.ifEmpty { "Beet" })
    }
}

data class PlantArea(
    val id: String = UUID.randomUUID().toString(),
    val plantId: String,
    val plantName: String,
    val plantEmoji: String? = null,
    val position: CanvasPoint,
    val radiusCm: Int = 15 // Default plant radius
)

// ==================== UNDO/REDO ====================

sealed class CanvasAction {
    data class AddBed(val bed: CanvasBed) : CanvasAction()
    data class UpdateBed(val oldBed: CanvasBed, val newBed: CanvasBed) : CanvasAction()
    data class DeleteBed(val bed: CanvasBed) : CanvasAction()
    data class AddPlant(val bedId: String, val plantArea: PlantArea) : CanvasAction()
    data class RemovePlant(val bedId: String, val plantArea: PlantArea) : CanvasAction()
}

// ==================== CANVAS STATE ====================

enum class CanvasMode {
    IDLE,           // Normal view - tap to select, pinch to zoom
    DRAWING_BED,    // Finger is drawing a new bed
    BED_SELECTED,   // A bed is selected, show options
    DRAGGING_PLANT  // Dragging a plant from picker
}

data class GardenCanvasState(
    val gardenId: String = "",
    val gardenName: String = "",
    val gardenWidthCm: Int = 500,
    val gardenHeightCm: Int = 500,
    val beds: List<CanvasBed> = emptyList(),
    val mode: CanvasMode = CanvasMode.IDLE,
    val selectedBedId: String? = null,
    val currentDrawingPoints: List<CanvasPoint> = emptyList(),
    val draggedPlantId: String? = null,
    val draggedPlantPosition: CanvasPoint? = null,
    val scale: Float = 1f,
    val panOffset: Offset = Offset.Zero,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isLoading: Boolean = false,
    val showBedLabelOverlay: Boolean = false
) {
    val selectedBed: CanvasBed? get() = beds.find { it.id == selectedBedId }
}

// ==================== COLORS ====================

object BedColors {
    val colors = listOf(
        "#8D6E63", "#A1887F", "#6D4C41", // Browns
        "#81C784", "#66BB6A", "#43A047", // Greens
        "#FFB74D", "#FFA726", "#FB8C00", // Oranges
        "#90A4AE", "#78909C", "#607D8B"  // Blue-greys
    )
    
    fun random(): String = colors.random()
    
    fun parse(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF8D6E63)
    }
}

// ==================== PLANT EMOJIS ====================

object PlantEmojis {
    val map = mapOf(
        "tomate" to "🍅",
        "karotte" to "🥕",
        "möhre" to "🥕",
        "salat" to "🥬",
        "kopfsalat" to "🥬",
        "gurke" to "🥒",
        "zucchini" to "🥒",
        "paprika" to "🌶️",
        "zwiebel" to "🧅",
        "knoblauch" to "🧄",
        "kartoffel" to "🥔",
        "mais" to "🌽",
        "kürbis" to "🎃",
        "erdbeere" to "🍓",
        "bohne" to "🫘",
        "erbse" to "🫛",
        "kohl" to "🥬",
        "brokkoli" to "🥦",
        "aubergine" to "🍆",
        "radieschen" to "🔴",
        "spinat" to "🥬",
        "petersilie" to "🌿",
        "basilikum" to "🌿",
        "sonnenblume" to "🌻"
    )
    
    fun forPlant(name: String): String? {
        val lower = name.lowercase()
        return map.entries.find { lower.contains(it.key) }?.value
    }
}
