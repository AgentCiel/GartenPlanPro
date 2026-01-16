package com.gartenplan.pro.feature.garden.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.domain.model.Bed
import com.gartenplan.pro.domain.usecase.garden.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        viewModelScope.launch {
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
        }
    }

    fun setPixelsPerMeter(ppm: Float) {
        pixelsPerMeter = ppm
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
     */
    fun onNavigationPan(deltaPx: Offset) {
        if (_state.value.mode != EditorMode.BEWEGUNG) return
        _state.value = _state.value.copy(
            panOffset = _state.value.panOffset + deltaPx / _state.value.scale
        )
    }

    /**
     * Zoom im Bewegungsmodus (2 Finger)
     */
    fun onNavigationZoom(factor: Float) {
        if (_state.value.mode != EditorMode.BEWEGUNG) return
        _state.value = _state.value.copy(
            scale = (_state.value.scale * factor).coerceIn(0.5f, 4f)
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

    // ==================== BUILD-MODUS: TOUCH START ====================

    fun onBuildTouchStart(positionPx: Offset) {
        if (_state.value.mode != EditorMode.BUILD) return
        
        val posM = pxToMeters(positionPx)
        
        when (_state.value.tool) {
            BuildTool.ADD_BED, BuildTool.ADD_PATH -> {
                // Zeichnen starten
                _state.value = _state.value.copy(
                    isDrawing = true,
                    drawStart = posM,
                    drawCurrent = posM
                )
            }
            BuildTool.SELECT -> {
                val hitBed = _state.value.beds.find { it.contains(posM) }
                
                if (hitBed != null) {
                    // Prüfen ob Ecke getroffen (für Resize)
                    val corner = findCorner(hitBed, posM)
                    
                    if (corner != null) {
                        // Resize starten
                        _state.value = _state.value.copy(
                            selectedBedId = hitBed.id,
                            isResizing = true,
                            activeCorner = corner,
                            dragStart = posM
                        )
                    } else {
                        // Drag starten
                        _state.value = _state.value.copy(
                            selectedBedId = hitBed.id,
                            isDragging = true,
                            dragStart = posM
                        )
                    }
                } else {
                    // Nichts getroffen
                    _state.value = _state.value.copy(selectedBedId = null)
                }
            }
        }
    }

    // ==================== BUILD-MODUS: TOUCH MOVE ====================

    fun onBuildTouchMove(positionPx: Offset) {
        if (_state.value.mode != EditorMode.BUILD) return
        
        val posM = pxToMeters(positionPx)
        val current = _state.value
        
        when {
            current.isDrawing -> {
                // Preview aktualisieren
                _state.value = current.copy(drawCurrent = posM)
            }
            current.isDragging -> {
                moveBed(posM)
            }
            current.isResizing -> {
                resizeBed(posM)
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
        }
    }

    private fun finishDrawing() {
        val start = _state.value.drawStart ?: return
        val end = _state.value.drawCurrent ?: return
        
        when (_state.value.tool) {
            BuildTool.ADD_BED -> {
                val width = kotlin.math.abs(end.x - start.x)
                val height = kotlin.math.abs(end.y - start.y)
                
                // Mindestgröße: 20cm x 20cm
                if (width >= 0.2f && height >= 0.2f) {
                    val bed = EditorBed(
                        x = clampX(minOf(start.x, end.x), width),
                        y = clampY(minOf(start.y, end.y), height),
                        width = width.coerceAtMost(_state.value.gardenWidthM),
                        height = height.coerceAtMost(_state.value.gardenHeightM)
                    )
                    
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
        
        val newX = clampX(bed.x + deltaX, bed.width)
        val newY = clampY(bed.y + deltaY, bed.height)
        
        val movedBed = bed.copy(x = newX, y = newY)
        
        _state.value = _state.value.copy(
            beds = _state.value.beds.map { if (it.id == bed.id) movedBed else it },
            dragStart = currentPosM
        )
    }

    private fun finishDrag() {
        _state.value.selectedBed?.let { bed ->
            // Nur persistieren, Undo wurde beim Start vorbereitet
            persistBed(bed)
        }
        
        _state.value = _state.value.copy(
            isDragging = false,
            dragStart = null
        )
    }

    private fun resizeBed(currentPosM: Offset) {
        val bed = _state.value.selectedBed ?: return
        val corner = _state.value.activeCorner ?: return
        
        val minSize = 0.2f  // Mindestens 20cm
        
        val newBounds = when (corner) {
            ResizeCorner.TOP_LEFT -> Rect(
                left = currentPosM.x.coerceIn(0f, bed.bounds.right - minSize),
                top = currentPosM.y.coerceIn(0f, bed.bounds.bottom - minSize),
                right = bed.bounds.right,
                bottom = bed.bounds.bottom
            )
            ResizeCorner.TOP_RIGHT -> Rect(
                left = bed.bounds.left,
                top = currentPosM.y.coerceIn(0f, bed.bounds.bottom - minSize),
                right = currentPosM.x.coerceIn(bed.bounds.left + minSize, _state.value.gardenWidthM),
                bottom = bed.bounds.bottom
            )
            ResizeCorner.BOTTOM_LEFT -> Rect(
                left = currentPosM.x.coerceIn(0f, bed.bounds.right - minSize),
                top = bed.bounds.top,
                right = bed.bounds.right,
                bottom = currentPosM.y.coerceIn(bed.bounds.top + minSize, _state.value.gardenHeightM)
            )
            ResizeCorner.BOTTOM_RIGHT -> Rect(
                left = bed.bounds.left,
                top = bed.bounds.top,
                right = currentPosM.x.coerceIn(bed.bounds.left + minSize, _state.value.gardenWidthM),
                bottom = currentPosM.y.coerceIn(bed.bounds.top + minSize, _state.value.gardenHeightM)
            )
        }
        
        val resizedBed = bed.copy(
            x = newBounds.left,
            y = newBounds.top,
            width = newBounds.width,
            height = newBounds.height
        )
        
        _state.value = _state.value.copy(
            beds = _state.value.beds.map { if (it.id == bed.id) resizedBed else it }
        )
    }

    private fun finishResize() {
        _state.value.selectedBed?.let { persistBed(it) }
        
        _state.value = _state.value.copy(
            isResizing = false,
            activeCorner = null,
            dragStart = null
        )
    }

    // ==================== BEET AKTIONEN ====================

    private fun addBed(bed: EditorBed) {
        executeAction(EditorAction.AddBed(bed))
        persistBed(bed)
    }

    fun deleteBed(bedId: String) {
        val bed = _state.value.beds.find { it.id == bedId } ?: return
        executeAction(EditorAction.DeleteBed(bed))
        
        viewModelScope.launch {
            deleteBedUseCase(bedId)
        }
        
        _state.value = _state.value.copy(selectedBedId = null)
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
     */
    fun onBuildPanZoom(pan: Offset, zoom: Float) {
        if (_state.value.mode != EditorMode.BUILD) return
        if (_state.value.isDragging || _state.value.isResizing || _state.value.isDrawing) return
        
        _state.value = _state.value.copy(
            scale = (_state.value.scale * zoom).coerceIn(0.5f, 4f),
            panOffset = _state.value.panOffset + pan / _state.value.scale
        )
    }

    fun resetView() {
        _state.value = _state.value.copy(
            scale = 1f,
            panOffset = Offset.Zero
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

    private fun updateUndoState() {
        _state.value = _state.value.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    // ==================== HELPER ====================

    private fun pxToMeters(px: Offset): Offset {
        val scale = _state.value.scale
        val pan = _state.value.panOffset
        return Offset(
            (px.x / scale - pan.x) / pixelsPerMeter,
            (px.y / scale - pan.y) / pixelsPerMeter
        )
    }

    private fun findCorner(bed: EditorBed, posM: Offset): ResizeCorner? {
        val tolerance = 0.15f  // 15cm Toleranz
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

    private fun clampX(x: Float, width: Float): Float =
        x.coerceIn(0f, _state.value.gardenWidthM - width)

    private fun clampY(y: Float, height: Float): Float =
        y.coerceIn(0f, _state.value.gardenHeightM - height)

    private fun persistBed(bed: EditorBed) {
        viewModelScope.launch {
            try {
                val domainBed = Bed(
                    id = bed.id,
                    gardenId = _state.value.gardenId,
                    name = bed.name.ifEmpty { "Beet" },
                    positionX = (bed.x * 100).toInt(),
                    positionY = (bed.y * 100).toInt(),
                    widthCm = (bed.width * 100).toInt(),
                    heightCm = (bed.height * 100).toInt(),
                    colorHex = bed.colorHex
                )
                updateBedUseCase(domainBed)
            } catch (e: Exception) {
                // Neues Beet erstellen
                createBedUseCase(
                    gardenId = _state.value.gardenId,
                    name = bed.name.ifEmpty { "Beet" },
                    positionX = (bed.x * 100).toInt(),
                    positionY = (bed.y * 100).toInt(),
                    widthCm = (bed.width * 100).toInt(),
                    heightCm = (bed.height * 100).toInt(),
                    colorHex = bed.colorHex
                )
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
        colorHex = colorHex
    )
}
