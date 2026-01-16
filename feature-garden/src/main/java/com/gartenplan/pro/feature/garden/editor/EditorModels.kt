package com.gartenplan.pro.feature.garden.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.gartenplan.pro.core.constants.BedShape
import java.util.UUID
import kotlin.math.sqrt

// ==================== EDITOR MODI ====================

/**
 * Die zwei Hauptmodi des Editors.
 * BEWEGUNG = Sicher, nur anschauen
 * BUILD = Verändern erlaubt
 */
enum class EditorMode {
    BEWEGUNG,  // Navigation: Pan/Zoom, nur Highlight
    BUILD      // Erstellen & Anpassen
}

/**
 * Werkzeuge im Build-Modus
 */
enum class BuildTool {
    SELECT,        // Objekt auswählen, verschieben, resizen
    ADD_BED,       // Neues rechteckiges Beet zeichnen
    ADD_CIRCLE_BED,// Neues rundes Beet zeichnen
    ADD_PATH       // Neuen Weg zeichnen
}

// ==================== GARTEN-OBJEKTE (in Metern!) ====================

/**
 * Ein Beet im Garten.
 * Alle Maße sind in METERN gespeichert.
 */
data class EditorBed(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val x: Float,           // Position X in Metern (für Kreise: Mittelpunkt X)
    val y: Float,           // Position Y in Metern (für Kreise: Mittelpunkt Y)
    val width: Float,       // Breite in Metern (für Kreise: Durchmesser)
    val height: Float,      // Höhe in Metern (für Kreise: Durchmesser)
    val colorHex: String = BedColors.random(),
    val plantIds: List<String> = emptyList(),
    val shape: BedShape = BedShape.RECTANGLE
) {
    // For circles, x/y represent top-left corner of bounding box (same as rect)
    val bounds: Rect get() = Rect(x, y, x + width, y + height)

    // For circles, use circle area formula
    val areaSqM: Float get() = when (shape) {
        BedShape.ROUND -> kotlin.math.PI.toFloat() * (radius * radius)
        else -> width * height
    }

    // Circle properties
    val isCircle: Boolean get() = shape == BedShape.ROUND
    val radius: Float get() = width / 2f  // For circles, width = height = diameter
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f

    fun contains(point: Offset): Boolean = when (shape) {
        BedShape.ROUND -> {
            // Distance from center to point
            val dx = point.x - centerX
            val dy = point.y - centerY
            sqrt(dx * dx + dy * dy) <= radius
        }
        else -> point.x in x..(x + width) && point.y in y..(y + height)
    }

    /**
     * Checks if this bed overlaps with another bed.
     * Handles rectangle-rectangle, circle-circle, and rectangle-circle cases.
     */
    fun overlaps(other: EditorBed): Boolean {
        if (this.id == other.id) return false
        val epsilon = 0.001f  // 1mm tolerance

        return when {
            // Both rectangles
            !this.isCircle && !other.isCircle -> {
                !(this.x + this.width <= other.x + epsilon ||
                        other.x + other.width <= this.x + epsilon ||
                        this.y + this.height <= other.y + epsilon ||
                        other.y + other.height <= this.y + epsilon)
            }
            // Both circles
            this.isCircle && other.isCircle -> {
                val dx = this.centerX - other.centerX
                val dy = this.centerY - other.centerY
                val distance = sqrt(dx * dx + dy * dy)
                distance < this.radius + other.radius - epsilon
            }
            // Circle and rectangle
            else -> {
                val circle = if (this.isCircle) this else other
                val rect = if (this.isCircle) other else this

                // Find the closest point on rectangle to circle center
                val closestX = circle.centerX.coerceIn(rect.x, rect.x + rect.width)
                val closestY = circle.centerY.coerceIn(rect.y, rect.y + rect.height)

                val dx = circle.centerX - closestX
                val dy = circle.centerY - closestY
                val distance = sqrt(dx * dx + dy * dy)

                distance < circle.radius - epsilon
            }
        }
    }

    fun displayName(): String = name.ifEmpty {
        if (isCircle) "Rundbeet" else "Beet"
    }
    fun areaText(): String = "%.1f m²".format(areaSqM)
    fun sizeText(): String = if (isCircle) {
        "⌀ %.1f m".format(width)
    } else {
        "%.1f × %.1f m".format(width, height)
    }
}

/**
 * Extension to find any overlapping bed in a list
 */
fun List<EditorBed>.findOverlapping(bed: EditorBed): EditorBed? =
    this.find { it.id != bed.id && it.overlaps(bed) }

/**
 * Extension to check if a bed would overlap with any existing bed
 */
fun List<EditorBed>.hasOverlap(bed: EditorBed): Boolean =
    this.any { it.id != bed.id && it.overlaps(bed) }

/**
 * Ein Weg im Garten.
 * Hat eine feste Breite, Länge ergibt sich aus Start/Ende.
 */
data class EditorPath(
    val id: String = UUID.randomUUID().toString(),
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val pathWidth: Float = 0.4f  // Feste Breite: 40cm
) {
    val lengthM: Float get() {
        val dx = endX - startX
        val dy = endY - startY
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    val bounds: Rect get() {
        val minX = minOf(startX, endX) - pathWidth / 2
        val maxX = maxOf(startX, endX) + pathWidth / 2
        val minY = minOf(startY, endY) - pathWidth / 2
        val maxY = maxOf(startY, endY) + pathWidth / 2
        return Rect(minX, minY, maxX, maxY)
    }
}

// ==================== EDITOR STATE ====================

/**
 * Welche Ecke wird zum Resizen gezogen?
 */
enum class ResizeCorner {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

/**
 * Der komplette Zustand des Garden Editors
 */
data class GardenEditorState(
    // Garten-Stammdaten
    val gardenId: String = "",
    val gardenName: String = "",
    val gardenWidthM: Float = 10f,   // Gartengröße in Metern
    val gardenHeightM: Float = 8f,

    // Objekte im Garten
    val beds: List<EditorBed> = emptyList(),
    val paths: List<EditorPath> = emptyList(),

    // Aktueller Modus
    val mode: EditorMode = EditorMode.BEWEGUNG,
    val tool: BuildTool = BuildTool.SELECT,

    // Auswahl & Highlight
    val selectedBedId: String? = null,
    val highlightedBedId: String? = null,  // Nur im Bewegungsmodus

    // Zeichenvorgang (für neues Beet/Weg)
    val isDrawing: Boolean = false,
    val drawStart: Offset? = null,      // In Metern
    val drawCurrent: Offset? = null,    // In Metern

    // Drag & Resize
    val isDragging: Boolean = false,
    val isResizing: Boolean = false,
    val activeCorner: ResizeCorner? = null,
    val dragStart: Offset? = null,

    // Overlap detection
    val hasOverlap: Boolean = false,

    // Snap lines (for visualization)
    val snapLinesX: List<Float> = emptyList(),
    val snapLinesY: List<Float> = emptyList(),

    // Kamera (für Pan/Zoom)
    val scale: Float = 1f,
    val panOffset: Offset = Offset.Zero,

    // Undo/Redo
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,

    // Loading
    val isLoading: Boolean = false,

    // User feedback message
    val userMessage: String? = null
) {
    val selectedBed: EditorBed? get() = beds.find { it.id == selectedBedId }

    // Preview-Rechteck beim Zeichnen (in Metern)
    val drawPreviewRect: Rect? get() {
        val start = drawStart ?: return null
        val current = drawCurrent ?: return null
        return Rect(
            left = minOf(start.x, current.x),
            top = minOf(start.y, current.y),
            right = maxOf(start.x, current.x),
            bottom = maxOf(start.y, current.y)
        )
    }

    // Preview circle data (center and radius in meters)
    val drawPreviewCircle: Triple<Float, Float, Float>? get() {
        if (tool != BuildTool.ADD_CIRCLE_BED) return null
        val start = drawStart ?: return null
        val current = drawCurrent ?: return null
        val dx = current.x - start.x
        val dy = current.y - start.y
        val radius = sqrt(dx * dx + dy * dy)
        return Triple(start.x, start.y, radius)
    }
}

// ==================== UNDO/REDO ====================

sealed class EditorAction {
    data class AddBed(val bed: EditorBed) : EditorAction()
    data class UpdateBed(val old: EditorBed, val new: EditorBed) : EditorAction()
    data class DeleteBed(val bed: EditorBed) : EditorAction()
    data class AddPath(val path: EditorPath) : EditorAction()
    data class DeletePath(val path: EditorPath) : EditorAction()
}

// ==================== FARBEN ====================

object BedColors {
    private val colors = listOf(
        "#8D6E63", "#A1887F", "#795548",  // Braun (Erde)
        "#81C784", "#66BB6A", "#4CAF50",  // Grün
        "#FFB74D", "#FFA726", "#FF9800"   // Orange
    )
    
    fun random(): String = colors.random()
    
    fun parse(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF795548)
    }
}

object PathColor {
    val default = Color(0xFFBDBDBD)  // Hellgrau
}

// ==================== SNAPPING ====================

/**
 * Result of a snap operation
 */
data class SnapResult(
    val snappedX: Float? = null,
    val snappedY: Float? = null,
    val snapLinesX: List<Float> = emptyList(),  // Vertical lines at X positions
    val snapLinesY: List<Float> = emptyList()   // Horizontal lines at Y positions
) {
    val hasSnap: Boolean get() = snappedX != null || snappedY != null
}

/**
 * Helper object for snapping beds to edges and other beds
 */
object SnapHelper {
    const val SNAP_THRESHOLD_M = 0.05f  // 5cm snap threshold

    /**
     * Find snap points for a bed being moved/resized
     * @param bedBounds The bounds of the bed being moved
     * @param otherBeds Other beds to snap to
     * @param gardenWidth Garden width for edge snapping
     * @param gardenHeight Garden height for edge snapping
     * @param excludeBedId ID of bed being moved (to exclude from snapping)
     */
    fun findSnapPoints(
        bedBounds: Rect,
        otherBeds: List<EditorBed>,
        gardenWidth: Float,
        gardenHeight: Float,
        excludeBedId: String? = null
    ): SnapResult {
        val snapLinesX = mutableListOf<Float>()
        val snapLinesY = mutableListOf<Float>()
        var snappedX: Float? = null
        var snappedY: Float? = null

        // Collect all edge positions to snap to
        val xEdges = mutableListOf(0f, gardenWidth)  // Garden edges
        val yEdges = mutableListOf(0f, gardenHeight)

        otherBeds.filter { it.id != excludeBedId }.forEach { bed ->
            xEdges.add(bed.x)               // Left edge
            xEdges.add(bed.x + bed.width)   // Right edge
            yEdges.add(bed.y)               // Top edge
            yEdges.add(bed.y + bed.height)  // Bottom edge
        }

        // Check left edge of moving bed
        for (edge in xEdges) {
            if (kotlin.math.abs(bedBounds.left - edge) < SNAP_THRESHOLD_M) {
                snappedX = edge
                snapLinesX.add(edge)
                break
            }
        }

        // If left didn't snap, check right edge
        if (snappedX == null) {
            for (edge in xEdges) {
                if (kotlin.math.abs(bedBounds.right - edge) < SNAP_THRESHOLD_M) {
                    snappedX = edge - bedBounds.width
                    snapLinesX.add(edge)
                    break
                }
            }
        }

        // Check top edge
        for (edge in yEdges) {
            if (kotlin.math.abs(bedBounds.top - edge) < SNAP_THRESHOLD_M) {
                snappedY = edge
                snapLinesY.add(edge)
                break
            }
        }

        // If top didn't snap, check bottom edge
        if (snappedY == null) {
            for (edge in yEdges) {
                if (kotlin.math.abs(bedBounds.bottom - edge) < SNAP_THRESHOLD_M) {
                    snappedY = edge - bedBounds.height
                    snapLinesY.add(edge)
                    break
                }
            }
        }

        return SnapResult(snappedX, snappedY, snapLinesX, snapLinesY)
    }
}
