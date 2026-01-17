package com.gartenplan.pro.feature.garden.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.core.constants.BedShape
import com.gartenplan.pro.domain.model.Bed
import com.gartenplan.pro.domain.usecase.garden.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class GardenEditorViewModel @Inject constructor(
    private val observeGardenUseCase: ObserveGardenUseCase,
    private val getBedsByGardenUseCase: GetBedsByGardenUseCase,
    private val createGardenUseCase: CreateGardenUseCase,
    private val createBedUseCase: CreateBedUseCase,
    private val updateBedUseCase: UpdateBedUseCase,
    private val deleteBedUseCase: DeleteBedUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(GardenEditorState())
    val state: StateFlow<GardenEditorState> = _state.asStateFlow()

    private val undoStack = mutableListOf<EditorAction>()
    private val redoStack = mutableListOf<EditorAction>()

    // Pixel pro Meter - wird vom Screen gesetzt
    private var pixelsPerMeter: Float = 100f
    private var canvasWidthPx: Float = 0f
    private var canvasHeightPx: Float = 0f

    // Flag um doppeltes Erstellen zu verhindern
    private var gardenCreationStarted = false

    // ==================== GARTEN LADEN ====================

    fun loadGarden(gardenId: String) {
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            combine(
                observeGardenUseCase(gardenId),
                getBedsByGardenUseCase(gardenId)
            ) { garden, beds -> Pair(garden, beds) }
            .collect { (garden, beds) ->
                garden?.let {
                    // Bekannte Bed-IDs merken (für Persistenz-Logik)
                    knownBedIds.clear()
                    beds.forEach { bed -> knownBedIds.add(bed.id) }

                    _state.value = _state.value.copy(
                        gardenId = it.id,
                        gardenName = it.name,
                        gardenWidthM = it.widthCm / 100f,
                        gardenHeightM = it.heightCm / 100f,
                        beds = beds.map { bed -> bed.toEditorBed() },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun createNewGarden(name: String, widthM: Float, heightM: Float) {
        // Verhindere doppeltes Erstellen durch Recomposition
        if (gardenCreationStarted) {
            android.util.Log.d("GardenEditor", "Garden creation already in progress, skipping duplicate call")
            return
        }
        gardenCreationStarted = true

        viewModelScope.launch {
            try {
                val id = createGardenUseCase(
                    name = name,
                    widthCm = (widthM * 100).toInt(),
                    heightCm = (heightM * 100).toInt()
                )
                _state.value = _state.value.copy(
                    gardenId = id,
                    gardenName = name,
                    gardenWidthM = widthM,
                    gardenHeightM = heightM,
                    isLoading = false
                )
                android.util.Log.d("GardenEditor", "Garden created successfully: $id")
            } catch (e: Exception) {
                android.util.Log.e("GardenEditor", "Failed to create garden: ${e.message}", e)
                gardenCreationStarted = false  // Reset bei Fehler
            }
        }
    }

    fun setPixelsPerMeter(ppm: Float) {
        pixelsPerMeter = ppm
    }

    /**
     * Setzt die Canvas-Größe für korrekte Zentrierung und Pan-Grenzen
     */
    fun setCanvasSize(widthPx: Float, heightPx: Float) {
        canvasWidthPx = widthPx
        canvasHeightPx = heightPx
    }

    // ==================== MODUS WECHSEL ====================

    /**
     * Wechselt zwischen BEWEGUNG und BUILD Modus
     */
    fun toggleMode() {
        val newMode = if (_state.value.mode == EditorMode.BEWEGUNG) 
            EditorMode.BUILD else EditorMode.BEWEGUNG
        
        _state.value = _state.value.copy(
            mode = newMode,
            // Bei Wechsel zu BEWEGUNG: alles zurücksetzen
            selectedBedId = if (newMode == EditorMode.BEWEGUNG) null else _state.value.selectedBedId,
            highlightedBedId = null,
            isDrawing = false,
            isDragging = false,
            isResizing = false,
            tool = if (newMode == EditorMode.BUILD) BuildTool.SELECT else BuildTool.SELECT
        )
    }

    fun setTool(tool: BuildTool) {
        if (_state.value.mode != EditorMode.BUILD) return
        _state.value = _state.value.copy(
            tool = tool,
            selectedBedId = null,
            isDrawing = false
        )
    }

    // ==================== BEWEGUNGSMODUS: GESTEN ====================

    /**
     * Pan im Bewegungsmodus (1 Finger)
     * WICHTIG: Begrenzt auf Gartenfläche + kleiner Rand
     */
    fun onNavigationPan(deltaPx: Offset) {
        if (_state.value.mode != EditorMode.BEWEGUNG) return
        val newPan = _state.value.panOffset + deltaPx / _state.value.scale
        _state.value = _state.value.copy(
            panOffset = clampPan(newPan, _state.value.scale)
        )
    }

    /**
     * Zoom im Bewegungsmodus (2 Finger)
     */
    fun onNavigationZoom(factor: Float) {
        if (_state.value.mode != EditorMode.BEWEGUNG) return
        val newScale = (_state.value.scale * factor).coerceIn(0.5f, 4f)
        // Nach Zoom: Pan neu begrenzen
        _state.value = _state.value.copy(
            scale = newScale,
            panOffset = clampPan(_state.value.panOffset, newScale)
        )
    }

    /**
     * Tap im Bewegungsmodus = nur Highlight
     */
    fun onNavigationTap(positionPx: Offset) {
        if (_state.value.mode != EditorMode.BEWEGUNG) return
        
        val posM = pxToMeters(positionPx)
        val hitBed = _state.value.beds.find { it.contains(posM) }
        
        _state.value = _state.value.copy(
            highlightedBedId = hitBed?.id
        )
    }

    // ==================== BUILD-MODUS: TAP ====================

    /**
     * Tap im Build-Modus = Beet auswählen (ohne zu ziehen)
     */
    fun onBuildTap(positionPx: Offset) {
        if (_state.value.mode != EditorMode.BUILD) return
        if (_state.value.tool != BuildTool.SELECT) return

        val posM = pxToMeters(positionPx)
        val hitBed = _state.value.beds.find { it.contains(posM) }

        _state.value = _state.value.copy(
            selectedBedId = hitBed?.id
        )
    }

    // ==================== BUILD-MODUS: TOUCH START ====================

    fun onBuildTouchStart(positionPx: Offset) {
        if (_state.value.mode != EditorMode.BUILD) return

        val posM = pxToMeters(positionPx)

        when (_state.value.tool) {
            BuildTool.ADD_BED, BuildTool.ADD_CIRCLE_BED, BuildTool.ADD_PATH -> {
                // Zeichnen starten
                _state.value = _state.value.copy(
                    isDrawing = true,
                    drawStart = posM,
                    drawCurrent = posM
                )
            }
            BuildTool.SELECT -> {
                // Bei SELECT: Nur Position merken, NICHT sofort Drag starten
                // Drag wird erst in onBuildTouchMove gestartet wenn genug Bewegung
                val hitBed = _state.value.beds.find { it.contains(posM) }

                if (hitBed != null) {
                    // Prüfen ob Ecke getroffen (für Resize)
                    val corner = findCorner(hitBed, posM)

                    // Nur vorbereiten, nicht starten
                    _state.value = _state.value.copy(
                        selectedBedId = hitBed.id,
                        activeCorner = corner,  // Merken welche Ecke (kann null sein)
                        dragStart = posM,       // Startposition merken
                        isDragging = false,     // Noch nicht dragging!
                        isResizing = false
                    )
                } else {
                    // Nichts getroffen - Auswahl aufheben
                    _state.value = _state.value.copy(
                        selectedBedId = null,
                        dragStart = null,
                        activeCorner = null
                    )
                }
            }
        }
    }

    // ==================== BUILD-MODUS: TOUCH MOVE ====================

    // Mindestbewegung bevor Drag/Resize startet (in Metern)
    private val dragThresholdM = 0.03f  // 3cm

    fun onBuildTouchMove(positionPx: Offset) {
        if (_state.value.mode != EditorMode.BUILD) return

        val posM = pxToMeters(positionPx)
        val current = _state.value

        when {
            current.isDrawing -> {
                // Preview aktualisieren and check for overlap
                val previewBed = when (current.tool) {
                    BuildTool.ADD_BED -> createPreviewBed(current.drawStart, posM)
                    BuildTool.ADD_CIRCLE_BED -> createPreviewCircleBed(current.drawStart, posM)
                    else -> null
                }
                val hasOverlap = previewBed?.let { current.beds.hasOverlap(it) } ?: false
                _state.value = current.copy(drawCurrent = posM, hasOverlap = hasOverlap)
            }
            current.isDragging -> {
                moveBed(posM)
            }
            current.isResizing -> {
                resizeBed(posM)
            }
            // Noch nicht dragging, aber Beet ausgewählt und dragStart gesetzt?
            // → Prüfen ob genug Bewegung für Drag/Resize
            current.selectedBedId != null && current.dragStart != null -> {
                val dx = posM.x - current.dragStart.x
                val dy = posM.y - current.dragStart.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                if (distance >= dragThresholdM) {
                    // Genug Bewegung - jetzt Drag oder Resize starten
                    if (current.activeCorner != null) {
                        // Resize starten
                        _state.value = current.copy(isResizing = true)
                        resizeBed(posM)
                    } else {
                        // Drag starten
                        _state.value = current.copy(isDragging = true)
                        moveBed(posM)
                    }
                }
            }
        }
    }

    // ==================== BUILD-MODUS: TOUCH END ====================

    fun onBuildTouchEnd() {
        if (_state.value.mode != EditorMode.BUILD) return

        val current = _state.value

        when {
            current.isDrawing -> finishDrawing()
            current.isDragging -> finishDrag()
            current.isResizing -> finishResize()
            // Wenn dragStart gesetzt aber kein Drag/Resize gestartet wurde
            // → War nur ein Tap, State aufräumen
            current.dragStart != null -> {
                _state.value = current.copy(
                    dragStart = null,
                    activeCorner = null
                )
            }
        }
    }

    private fun createPreviewBed(start: Offset?, end: Offset?): EditorBed? {
        if (start == null || end == null) return null
        val rawWidth = kotlin.math.abs(end.x - start.x)
        val rawHeight = kotlin.math.abs(end.y - start.y)
        val clampedWidth = rawWidth.coerceAtMost(_state.value.gardenWidthM)
        val clampedHeight = rawHeight.coerceAtMost(_state.value.gardenHeightM)
        if (clampedWidth < 0.2f || clampedHeight < 0.2f) return null

        val rawX = minOf(start.x, end.x).coerceAtLeast(0f)
        val rawY = minOf(start.y, end.y).coerceAtLeast(0f)

        return EditorBed(
            x = clampX(rawX, clampedWidth),
            y = clampY(rawY, clampedHeight),
            width = clampedWidth,
            height = clampedHeight,
            shape = BedShape.RECTANGLE
        )
    }

    private fun createPreviewCircleBed(start: Offset?, end: Offset?): EditorBed? {
        if (start == null || end == null) return null

        // Calculate radius as distance from start to current point
        val dx = end.x - start.x
        val dy = end.y - start.y
        val radius = sqrt(dx * dx + dy * dy)

        // Minimum radius 10cm (20cm diameter)
        if (radius < 0.1f) return null

        val diameter = radius * 2

        // Calculate top-left corner of bounding box
        val rawX = (start.x - radius).coerceAtLeast(0f)
        val rawY = (start.y - radius).coerceAtLeast(0f)

        // Clamp to garden bounds
        val clampedDiameter = minOf(
            diameter,
            _state.value.gardenWidthM - rawX,
            _state.value.gardenHeightM - rawY,
            _state.value.gardenWidthM,
            _state.value.gardenHeightM
        )

        if (clampedDiameter < 0.2f) return null

        return EditorBed(
            x = clampX(rawX, clampedDiameter),
            y = clampY(rawY, clampedDiameter),
            width = clampedDiameter,
            height = clampedDiameter,
            shape = BedShape.ROUND
        )
    }

    private fun finishDrawing() {
        val start = _state.value.drawStart ?: return
        val end = _state.value.drawCurrent ?: return

        when (_state.value.tool) {
            BuildTool.ADD_BED -> {
                val bed = createPreviewBed(start, end)

                if (bed != null) {
                    // Check for overlap - don't create if overlapping
                    if (_state.value.beds.hasOverlap(bed)) {
                        // Reset drawing state but don't create bed
                        _state.value = _state.value.copy(
                            isDrawing = false,
                            drawStart = null,
                            drawCurrent = null,
                            hasOverlap = false
                        )
                        return
                    }

                    addBed(bed)

                    // Automatisch auswählen und zu SELECT wechseln
                    _state.value = _state.value.copy(
                        selectedBedId = bed.id,
                        tool = BuildTool.SELECT
                    )
                }
            }
            BuildTool.ADD_CIRCLE_BED -> {
                val bed = createPreviewCircleBed(start, end)

                if (bed != null) {
                    // Check for overlap - don't create if overlapping
                    if (_state.value.beds.hasOverlap(bed)) {
                        _state.value = _state.value.copy(
                            isDrawing = false,
                            drawStart = null,
                            drawCurrent = null,
                            hasOverlap = false
                        )
                        return
                    }

                    addBed(bed)

                    // Automatisch auswählen und zu SELECT wechseln
                    _state.value = _state.value.copy(
                        selectedBedId = bed.id,
                        tool = BuildTool.SELECT
                    )
                }
            }
            BuildTool.ADD_PATH -> {
                val length = kotlin.math.sqrt(
                    (end.x - start.x).let { it * it } + 
                    (end.y - start.y).let { it * it }
                )
                
                if (length >= 0.3f) {
                    val path = EditorPath(
                        startX = start.x,
                        startY = start.y,
                        endX = end.x,
                        endY = end.y
                    )
                    
                    // Prüfen ob Weg über Beet verläuft
                    val overlaps = _state.value.beds.any { bed ->
                        bed.bounds.overlaps(path.bounds)
                    }
                    
                    if (!overlaps) {
                        executeAction(EditorAction.AddPath(path))
                    }
                    // TODO: Hinweis wenn Weg über Beet
                }
            }
            else -> {}
        }
        
        // Reset
        _state.value = _state.value.copy(
            isDrawing = false,
            drawStart = null,
            drawCurrent = null
        )
    }

    private fun moveBed(currentPosM: Offset) {
        val bed = _state.value.selectedBed ?: return
        val start = _state.value.dragStart ?: return

        val deltaX = currentPosM.x - start.x
        val deltaY = currentPosM.y - start.y

        var newX = clampX(bed.x + deltaX, bed.width)
        var newY = clampY(bed.y + deltaY, bed.height)

        // Apply snapping
        val candidateBounds = Rect(newX, newY, newX + bed.width, newY + bed.height)
        val otherBeds = _state.value.beds.filter { it.id != bed.id }
        val snapResult = SnapHelper.findSnapPoints(
            bedBounds = candidateBounds,
            otherBeds = otherBeds,
            gardenWidth = _state.value.gardenWidthM,
            gardenHeight = _state.value.gardenHeightM,
            excludeBedId = bed.id
        )

        // Apply snapped positions
        if (snapResult.snappedX != null) {
            newX = clampX(snapResult.snappedX, bed.width)
        }
        if (snapResult.snappedY != null) {
            newY = clampY(snapResult.snappedY, bed.height)
        }

        val movedBed = bed.copy(x = newX, y = newY)

        // Check for overlap with other beds
        val hasOverlap = otherBeds.hasOverlap(movedBed)

        if (!hasOverlap) {
            _state.value = _state.value.copy(
                beds = _state.value.beds.map { if (it.id == bed.id) movedBed else it },
                dragStart = currentPosM,
                hasOverlap = false,
                snapLinesX = snapResult.snapLinesX,
                snapLinesY = snapResult.snapLinesY
            )
        } else {
            // Show overlap indicator but don't move
            _state.value = _state.value.copy(
                hasOverlap = true,
                snapLinesX = emptyList(),
                snapLinesY = emptyList()
            )
        }
    }

    private fun finishDrag() {
        _state.value.selectedBed?.let { bed ->
            // Nur persistieren, Undo wurde beim Start vorbereitet
            persistBed(bed)
        }

        _state.value = _state.value.copy(
            isDragging = false,
            dragStart = null,
            hasOverlap = false,
            snapLinesX = emptyList(),
            snapLinesY = emptyList()
        )
    }

    private fun resizeBed(currentPosM: Offset) {
        val bed = _state.value.selectedBed ?: return
        val corner = _state.value.activeCorner ?: return

        val minSize = 0.2f  // Mindestens 20cm
        val snapThreshold = SnapHelper.SNAP_THRESHOLD_M
        val otherBeds = _state.value.beds.filter { it.id != bed.id }
        val gardenWidth = _state.value.gardenWidthM
        val gardenHeight = _state.value.gardenHeightM

        // Sammle alle Snap-Kanten
        val xEdges = mutableListOf(0f, gardenWidth)
        val yEdges = mutableListOf(0f, gardenHeight)
        otherBeds.forEach { other ->
            xEdges.add(other.x)
            xEdges.add(other.x + other.width)
            yEdges.add(other.y)
            yEdges.add(other.y + other.height)
        }

        // Snap-Funktion für einzelne Werte
        fun snapTo(value: Float, edges: List<Float>): Pair<Float, Float?> {
            for (edge in edges) {
                if (kotlin.math.abs(value - edge) < snapThreshold) {
                    return Pair(edge, edge)
                }
            }
            return Pair(value, null)
        }

        val snapLinesX = mutableListOf<Float>()
        val snapLinesY = mutableListOf<Float>()

        val newBounds = when (corner) {
            ResizeCorner.TOP_LEFT -> {
                val (snappedLeft, lineX) = snapTo(currentPosM.x.coerceIn(0f, bed.bounds.right - minSize), xEdges)
                val (snappedTop, lineY) = snapTo(currentPosM.y.coerceIn(0f, bed.bounds.bottom - minSize), yEdges)
                lineX?.let { snapLinesX.add(it) }
                lineY?.let { snapLinesY.add(it) }
                Rect(left = snappedLeft, top = snappedTop, right = bed.bounds.right, bottom = bed.bounds.bottom)
            }
            ResizeCorner.TOP_RIGHT -> {
                val (snappedRight, lineX) = snapTo(currentPosM.x.coerceIn(bed.bounds.left + minSize, gardenWidth), xEdges)
                val (snappedTop, lineY) = snapTo(currentPosM.y.coerceIn(0f, bed.bounds.bottom - minSize), yEdges)
                lineX?.let { snapLinesX.add(it) }
                lineY?.let { snapLinesY.add(it) }
                Rect(left = bed.bounds.left, top = snappedTop, right = snappedRight, bottom = bed.bounds.bottom)
            }
            ResizeCorner.BOTTOM_LEFT -> {
                val (snappedLeft, lineX) = snapTo(currentPosM.x.coerceIn(0f, bed.bounds.right - minSize), xEdges)
                val (snappedBottom, lineY) = snapTo(currentPosM.y.coerceIn(bed.bounds.top + minSize, gardenHeight), yEdges)
                lineX?.let { snapLinesX.add(it) }
                lineY?.let { snapLinesY.add(it) }
                Rect(left = snappedLeft, top = bed.bounds.top, right = bed.bounds.right, bottom = snappedBottom)
            }
            ResizeCorner.BOTTOM_RIGHT -> {
                val (snappedRight, lineX) = snapTo(currentPosM.x.coerceIn(bed.bounds.left + minSize, gardenWidth), xEdges)
                val (snappedBottom, lineY) = snapTo(currentPosM.y.coerceIn(bed.bounds.top + minSize, gardenHeight), yEdges)
                lineX?.let { snapLinesX.add(it) }
                lineY?.let { snapLinesY.add(it) }
                Rect(left = bed.bounds.left, top = bed.bounds.top, right = snappedRight, bottom = snappedBottom)
            }
        }

        val resizedBed = bed.copy(
            x = newBounds.left,
            y = newBounds.top,
            width = newBounds.width,
            height = newBounds.height
        )

        // Check for overlap with other beds
        val hasOverlap = otherBeds.hasOverlap(resizedBed)

        if (!hasOverlap) {
            _state.value = _state.value.copy(
                beds = _state.value.beds.map { if (it.id == bed.id) resizedBed else it },
                hasOverlap = false,
                snapLinesX = snapLinesX,
                snapLinesY = snapLinesY
            )
        } else {
            // Show overlap indicator but don't resize
            _state.value = _state.value.copy(
                hasOverlap = true,
                snapLinesX = emptyList(),
                snapLinesY = emptyList()
            )
        }
    }

    private fun finishResize() {
        _state.value.selectedBed?.let { persistBed(it) }

        _state.value = _state.value.copy(
            isResizing = false,
            activeCorner = null,
            dragStart = null,
            hasOverlap = false,
            snapLinesX = emptyList(),
            snapLinesY = emptyList()
        )
    }

    // ==================== BEET AKTIONEN ====================

    private fun addBed(bed: EditorBed) {
        executeAction(EditorAction.AddBed(bed))
        persistBed(bed)
    }

    fun deleteBed(bedId: String) {
        val bed = _state.value.beds.find { it.id == bedId } ?: return
        val bedName = bed.displayName()
        executeAction(EditorAction.DeleteBed(bed))

        viewModelScope.launch {
            deleteBedUseCase(bedId)
        }

        _state.value = _state.value.copy(selectedBedId = null)
        showMessage("$bedName gelöscht")
    }

    fun duplicateBed(bedId: String) {
        val bed = _state.value.beds.find { it.id == bedId } ?: return

        // Calculate offset - try to place to the right first, then down
        val offsetX = 0.2f  // 20cm offset
        val offsetY = 0.2f

        // Try different positions to find non-overlapping spot
        val positions = listOf(
            // Right of original
            Pair(bed.x + bed.width + offsetX, bed.y),
            // Below original
            Pair(bed.x, bed.y + bed.height + offsetY),
            // Diagonal
            Pair(bed.x + bed.width + offsetX, bed.y + bed.height + offsetY),
            // Left if space
            Pair(bed.x - bed.width - offsetX, bed.y),
            // Above if space
            Pair(bed.x, bed.y - bed.height - offsetY)
        )

        var duplicatedBed: EditorBed? = null

        for ((tryX, tryY) in positions) {
            val clampedX = clampX(tryX, bed.width)
            val clampedY = clampY(tryY, bed.height)

            val candidate = EditorBed(
                name = if (bed.name.isEmpty()) "" else "${bed.name} (Kopie)",
                x = clampedX,
                y = clampedY,
                width = bed.width,
                height = bed.height,
                colorHex = bed.colorHex,
                plantIds = emptyList(),
                shape = bed.shape  // Form beibehalten!
            )

            if (!_state.value.beds.hasOverlap(candidate)) {
                duplicatedBed = candidate
                break
            }
        }

        if (duplicatedBed == null) {
            // No valid position found - still create but it might overlap
            duplicatedBed = EditorBed(
                name = if (bed.name.isEmpty()) "" else "${bed.name} (Kopie)",
                x = clampX(bed.x + bed.width + offsetX, bed.width),
                y = clampY(bed.y, bed.height),
                width = bed.width,
                height = bed.height,
                colorHex = bed.colorHex,
                plantIds = emptyList(),
                shape = bed.shape  // Form beibehalten!
            )
        }

        addBed(duplicatedBed)

        // Select the new bed
        _state.value = _state.value.copy(selectedBedId = duplicatedBed.id)
        showMessage("Beet kopiert")
    }

    fun updateBedName(name: String) {
        val bed = _state.value.selectedBed ?: return
        val updated = bed.copy(name = name)
        
        executeAction(EditorAction.UpdateBed(bed, updated))
        persistBed(updated)
    }

    // ==================== BUILD-MODUS: PAN/ZOOM ====================

    /**
     * Im Build-Modus: Zwei-Finger-Geste für Pan/Zoom
     * WICHTIG: Nur wenn NICHT gerade ein Objekt bewegt wird!
     * Pan ist begrenzt auf Gartenfläche + Rand
     */
    fun onBuildPanZoom(pan: Offset, zoom: Float) {
        if (_state.value.mode != EditorMode.BUILD) return
        if (_state.value.isDragging || _state.value.isResizing || _state.value.isDrawing) return

        val newScale = (_state.value.scale * zoom).coerceIn(0.5f, 4f)
        val newPan = _state.value.panOffset + pan / _state.value.scale

        _state.value = _state.value.copy(
            scale = newScale,
            panOffset = clampPan(newPan, newScale)
        )
    }

    /**
     * Zentriert den Garten exakt in der Mitte des sichtbaren Bereichs.
     * Berechnet den optimalen Scale, damit der Garten vollständig sichtbar ist.
     */
    fun resetView() {
        if (canvasWidthPx <= 0 || canvasHeightPx <= 0) {
            // Fallback wenn Canvas-Größe noch nicht bekannt
            _state.value = _state.value.copy(scale = 1f, panOffset = Offset.Zero)
            return
        }

        val gardenWidthPx = _state.value.gardenWidthM * pixelsPerMeter
        val gardenHeightPx = _state.value.gardenHeightM * pixelsPerMeter

        // Berechne optimalen Scale, damit Garten vollständig sichtbar (mit etwas Rand)
        val padding = 40f  // 40px Rand
        val scaleX = (canvasWidthPx - padding * 2) / gardenWidthPx
        val scaleY = (canvasHeightPx - padding * 2) / gardenHeightPx
        val optimalScale = minOf(scaleX, scaleY).coerceIn(0.5f, 2f)

        // Berechne Pan um Garten zu zentrieren
        // Bei scale=1 und pan=0 ist der Garten oben-links
        // Wir wollen ihn in der Mitte des Canvas
        val centeredPanX = (canvasWidthPx - gardenWidthPx * optimalScale) / 2f / optimalScale
        val centeredPanY = (canvasHeightPx - gardenHeightPx * optimalScale) / 2f / optimalScale

        _state.value = _state.value.copy(
            scale = optimalScale,
            panOffset = Offset(centeredPanX, centeredPanY)
        )
    }

    // ==================== UNDO / REDO ====================

    private fun executeAction(action: EditorAction) {
        applyAction(action)
        undoStack.add(action)
        redoStack.clear()
        updateUndoState()
    }

    private fun applyAction(action: EditorAction) {
        _state.value = when (action) {
            is EditorAction.AddBed -> _state.value.copy(
                beds = _state.value.beds + action.bed
            )
            is EditorAction.UpdateBed -> _state.value.copy(
                beds = _state.value.beds.map { if (it.id == action.new.id) action.new else it }
            )
            is EditorAction.DeleteBed -> _state.value.copy(
                beds = _state.value.beds.filter { it.id != action.bed.id }
            )
            is EditorAction.AddPath -> _state.value.copy(
                paths = _state.value.paths + action.path
            )
            is EditorAction.DeletePath -> _state.value.copy(
                paths = _state.value.paths.filter { it.id != action.path.id }
            )
        }
    }

    private fun reverseAction(action: EditorAction) {
        _state.value = when (action) {
            is EditorAction.AddBed -> _state.value.copy(
                beds = _state.value.beds.filter { it.id != action.bed.id }
            )
            is EditorAction.UpdateBed -> _state.value.copy(
                beds = _state.value.beds.map { if (it.id == action.old.id) action.old else it }
            )
            is EditorAction.DeleteBed -> _state.value.copy(
                beds = _state.value.beds + action.bed
            )
            is EditorAction.AddPath -> _state.value.copy(
                paths = _state.value.paths.filter { it.id != action.path.id }
            )
            is EditorAction.DeletePath -> _state.value.copy(
                paths = _state.value.paths + action.path
            )
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val action = undoStack.removeLast()
        reverseAction(action)
        redoStack.add(action)
        updateUndoState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val action = redoStack.removeLast()
        applyAction(action)
        undoStack.add(action)
        updateUndoState()
    }

    fun clearMessage() {
        _state.value = _state.value.copy(userMessage = null)
    }

    private fun showMessage(message: String) {
        _state.value = _state.value.copy(userMessage = message)
    }

    private fun updateUndoState() {
        _state.value = _state.value.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    // ==================== HELPER ====================

    /**
     * Begrenzt Pan symmetrisch, damit der Garten nie komplett aus dem Sichtfeld verschwindet.
     * Der Garten bleibt immer mindestens zu 40% sichtbar.
     * Grenzen sind in alle Richtungen gleich!
     */
    private fun clampPan(pan: Offset, scale: Float): Offset {
        if (canvasWidthPx <= 0 || canvasHeightPx <= 0) {
            // Fallback wenn Canvas-Größe noch nicht bekannt
            val gardenWidthPx = _state.value.gardenWidthM * pixelsPerMeter
            val gardenHeightPx = _state.value.gardenHeightM * pixelsPerMeter
            val margin = maxOf(gardenWidthPx, gardenHeightPx) * 0.5f
            return Offset(
                pan.x.coerceIn(-margin, margin),
                pan.y.coerceIn(-margin, margin)
            )
        }

        val gardenWidthPx = _state.value.gardenWidthM * pixelsPerMeter
        val gardenHeightPx = _state.value.gardenHeightM * pixelsPerMeter

        // Symmetrischer Puffer: Garten darf maximal 60% außerhalb sein in jede Richtung
        // Das bedeutet mindestens 40% bleibt immer sichtbar
        val bufferFraction = 0.6f

        // Berechne zentrierte Position (wo der Garten mittig wäre)
        val centeredPanX = (canvasWidthPx - gardenWidthPx * scale) / 2f / scale
        val centeredPanY = (canvasHeightPx - gardenHeightPx * scale) / 2f / scale

        // Symmetrischer Spielraum um die Mitte herum
        val maxOffsetX = gardenWidthPx * bufferFraction
        val maxOffsetY = gardenHeightPx * bufferFraction

        return Offset(
            pan.x.coerceIn(centeredPanX - maxOffsetX, centeredPanX + maxOffsetX),
            pan.y.coerceIn(centeredPanY - maxOffsetY, centeredPanY + maxOffsetY)
        )
    }

    private fun pxToMeters(px: Offset): Offset {
        val scale = _state.value.scale
        val pan = _state.value.panOffset
        return Offset(
            (px.x / scale - pan.x) / pixelsPerMeter,
            (px.y / scale - pan.y) / pixelsPerMeter
        )
    }

    private fun findCorner(bed: EditorBed, posM: Offset): ResizeCorner? {
        // 25cm Toleranz für bessere Touch-Erkennung auf Mobilgeräten
        val tolerance = 0.25f
        val b = bed.bounds

        return when {
            distance(posM, Offset(b.left, b.top)) < tolerance -> ResizeCorner.TOP_LEFT
            distance(posM, Offset(b.right, b.top)) < tolerance -> ResizeCorner.TOP_RIGHT
            distance(posM, Offset(b.left, b.bottom)) < tolerance -> ResizeCorner.BOTTOM_LEFT
            distance(posM, Offset(b.right, b.bottom)) < tolerance -> ResizeCorner.BOTTOM_RIGHT
            else -> null
        }
    }

    private fun distance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun clampX(x: Float, width: Float): Float {
        val maxX = (_state.value.gardenWidthM - width).coerceAtLeast(0f)
        return x.coerceIn(0f, maxX)
    }

    private fun clampY(y: Float, height: Float): Float {
        val maxY = (_state.value.gardenHeightM - height).coerceAtLeast(0f)
        return y.coerceIn(0f, maxY)
    }

    // Speichert bekannte Bed-IDs, um zu wissen ob ein Beet neu ist
    private val knownBedIds = mutableSetOf<String>()

    private fun persistBed(bed: EditorBed) {
        val gardenId = _state.value.gardenId
        if (gardenId.isEmpty()) {
            android.util.Log.e("GardenEditor", "Cannot persist bed: gardenId is empty")
            return
        }

        viewModelScope.launch {
            try {
                val domainBed = Bed(
                    id = bed.id,
                    gardenId = gardenId,
                    name = bed.name.ifEmpty { if (bed.isCircle) "Rundbeet" else "Beet" },
                    positionX = (bed.x * 100).toInt(),
                    positionY = (bed.y * 100).toInt(),
                    widthCm = (bed.width * 100).toInt(),
                    heightCm = (bed.height * 100).toInt(),
                    shape = bed.shape,
                    colorHex = bed.colorHex
                )

                if (bed.id in knownBedIds) {
                    // Bekanntes Beet aktualisieren
                    updateBedUseCase(domainBed)
                    android.util.Log.d("GardenEditor", "Updated bed: ${bed.id}")
                } else {
                    // Neues Beet - direkt erstellen mit der EditorBed ID
                    createBedUseCase.createWithId(
                        id = bed.id,
                        gardenId = gardenId,
                        name = domainBed.name,
                        positionX = domainBed.positionX,
                        positionY = domainBed.positionY,
                        widthCm = domainBed.widthCm,
                        heightCm = domainBed.heightCm,
                        shape = domainBed.shape,
                        colorHex = domainBed.colorHex
                    )
                    knownBedIds.add(bed.id)
                    android.util.Log.d("GardenEditor", "Created new bed: ${bed.id}")
                }
            } catch (e: Exception) {
                android.util.Log.e("GardenEditor", "Failed to persist bed: ${e.message}", e)
                showMessage("Fehler beim Speichern des Beets")
            }
        }
    }

    private fun Bed.toEditorBed() = EditorBed(
        id = id,
        name = name,
        x = positionX / 100f,
        y = positionY / 100f,
        width = widthCm / 100f,
        height = heightCm / 100f,
        colorHex = colorHex,
        shape = shape
    )
}
