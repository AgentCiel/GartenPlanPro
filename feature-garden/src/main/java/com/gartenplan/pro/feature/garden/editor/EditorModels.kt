package com.gartenplan.pro.feature.garden.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import java.util.UUID

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
    SELECT,    // Objekt auswählen, verschieben, resizen
    ADD_BED,   // Neues Beet zeichnen
    ADD_PATH   // Neuen Weg zeichnen
}

// ==================== GARTEN-OBJEKTE (in Metern!) ====================

/**
 * Ein Beet im Garten.
 * Alle Maße sind in METERN gespeichert.
 */
data class EditorBed(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val x: Float,           // Position X in Metern
    val y: Float,           // Position Y in Metern
    val width: Float,       // Breite in Metern
    val height: Float,      // Höhe in Metern
    val colorHex: String = BedColors.random(),
    val plantIds: List<String> = emptyList()
) {
    val bounds: Rect get() = Rect(x, y, x + width, y + height)
    val areaSqM: Float get() = width * height
    
    fun contains(point: Offset): Boolean =
        point.x in x..(x + width) && point.y in y..(y + height)
    
    fun displayName(): String = name.ifEmpty { "Beet" }
    fun areaText(): String = "%.1f m²".format(areaSqM)
    fun sizeText(): String = "%.1f × %.1f m".format(width, height)
}

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
    
    // Kamera (für Pan/Zoom)
    val scale: Float = 1f,
    val panOffset: Offset = Offset.Zero,
    
    // Undo/Redo
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    
    // Loading
    val isLoading: Boolean = false
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
