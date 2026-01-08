package com.gartenplan.pro.feature.garden.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.domain.model.Bed
import com.gartenplan.pro.domain.model.Plant
import com.gartenplan.pro.domain.usecase.garden.*
import com.gartenplan.pro.domain.usecase.plant.GetAllPlantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class QuickBedShape { RECTANGLE, CIRCLE }

@HiltViewModel
class GardenCanvasViewModel @Inject constructor(
    private val observeGardenUseCase: ObserveGardenUseCase,
    private val getBedsByGardenUseCase: GetBedsByGardenUseCase,
    private val createGardenUseCase: CreateGardenUseCase,
    private val createBedUseCase: CreateBedUseCase,
    private val updateBedUseCase: UpdateBedUseCase,
    private val deleteBedUseCase: DeleteBedUseCase,
    private val getAllPlantsUseCase: GetAllPlantsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(GardenCanvasState())
    val state: StateFlow<GardenCanvasState> = _state.asStateFlow()

    private val _plants = MutableStateFlow<List<Plant>>(emptyList())
    val plants: StateFlow<List<Plant>> = _plants.asStateFlow()

    // Undo/Redo
    private val undoStack = mutableListOf<CanvasAction>()
    private val redoStack = mutableListOf<CanvasAction>()

    // Canvas dimensions
    private var canvasWidth: Float = 1000f
    private var canvasHeight: Float = 800f

    init {
        loadPlants()
    }

    private fun loadPlants() {
        viewModelScope.launch {
            getAllPlantsUseCase().collect { plantList ->
                _plants.value = plantList
            }
        }
    }

    // ==================== GARDEN LOADING ====================

    fun loadGarden(gardenId: String) {
        _state.value = _state.value.copy(isLoading = true)
        
        viewModelScope.launch {
            combine(
                observeGardenUseCase(gardenId),
                getBedsByGardenUseCase(gardenId)
            ) { garden, beds ->
                Pair(garden, beds)
            }.collect { (garden, beds) ->
                if (garden != null) {
                    _state.value = _state.value.copy(
                        gardenId = garden.id,
                        gardenName = garden.name,
                        gardenWidthCm = garden.widthCm,
                        gardenHeightCm = garden.heightCm,
                        beds = beds.map { it.toCanvasBed() },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun createNewGarden(name: String, widthCm: Int, heightCm: Int) {
        viewModelScope.launch {
            val gardenId = createGardenUseCase(
                name = name,
                widthCm = widthCm,
                heightCm = heightCm
            )
            _state.value = _state.value.copy(
                gardenId = gardenId,
                gardenName = name,
                gardenWidthCm = widthCm,
                gardenHeightCm = heightCm,
                beds = emptyList(),
                isLoading = false
            )
        }
    }

    fun setCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
    }

    // ==================== MODE CONTROL ====================

    fun toggleDrawMode() {
        val currentMode = _state.value.mode
        _state.value = _state.value.copy(
            mode = if (currentMode == CanvasMode.DRAWING_BED) CanvasMode.IDLE else CanvasMode.DRAWING_BED,
            currentDrawingPoints = emptyList(),
            selectedBedId = null,
            showBedLabelOverlay = false
        )
    }

    fun selectBed(bedId: String?) {
        _state.value = _state.value.copy(
            selectedBedId = bedId,
            mode = if (bedId != null) CanvasMode.BED_SELECTED else CanvasMode.IDLE,
            showBedLabelOverlay = false
        )
    }

    fun deselectBed() {
        _state.value = _state.value.copy(
            selectedBedId = null,
            mode = CanvasMode.IDLE,
            showBedLabelOverlay = false
        )
    }

    // ==================== DRAWING ====================

    fun onTouchStart(point: CanvasPoint) {
        if (_state.value.mode == CanvasMode.DRAWING_BED) {
            _state.value = _state.value.copy(
                currentDrawingPoints = listOf(point)
            )
        }
    }

    fun onTouchMove(point: CanvasPoint) {
        val currentState = _state.value
        
        if (currentState.mode == CanvasMode.DRAWING_BED) {
            val lastPoint = currentState.currentDrawingPoints.lastOrNull()
            if (lastPoint == null || point.distanceTo(lastPoint) > 3f) {
                _state.value = currentState.copy(
                    currentDrawingPoints = currentState.currentDrawingPoints + point
                )
            }
        } else if (currentState.mode == CanvasMode.DRAGGING_PLANT) {
            _state.value = currentState.copy(
                draggedPlantPosition = point
            )
        }
    }

    fun onTouchEnd() {
        val currentState = _state.value
        
        when (currentState.mode) {
            CanvasMode.DRAWING_BED -> finishDrawingBed()
            CanvasMode.DRAGGING_PLANT -> dropPlant()
            else -> {}
        }
    }

    private fun finishDrawingBed() {
        val points = _state.value.currentDrawingPoints
        
        if (points.size < 3) {
            _state.value = _state.value.copy(currentDrawingPoints = emptyList())
            return
        }

        val bounds = CanvasPolygon(points).bounds
        if (bounds.width < 30 || bounds.height < 30) {
            _state.value = _state.value.copy(currentDrawingPoints = emptyList())
            return
        }

        // Create smoothed polygon
        val polygon = CanvasPolygon(points).simplified(8f).smoothed(1)
        
        val newBed = CanvasBed(
            id = UUID.randomUUID().toString(),
            name = "",
            polygon = polygon,
            colorHex = BedColors.random()
        )

        executeAction(CanvasAction.AddBed(newBed))
        persistBed(newBed)

        // Stay in draw mode but clear points, select new bed for naming
        _state.value = _state.value.copy(
            mode = CanvasMode.BED_SELECTED,
            currentDrawingPoints = emptyList(),
            selectedBedId = newBed.id,
            showBedLabelOverlay = true
        )
    }

    // ==================== QUICK SHAPES ====================

    fun addQuickBed(shape: QuickBedShape) {
        // Place in center of visible canvas area
        val centerX = canvasWidth / 2 - _state.value.panOffset.x
        val centerY = canvasHeight / 2 - _state.value.panOffset.y
        
        val size = 150f // Default size in pixels
        
        val polygon = when (shape) {
            QuickBedShape.RECTANGLE -> createRectanglePolygon(centerX, centerY, size, size * 0.7f)
            QuickBedShape.CIRCLE -> createCirclePolygon(centerX, centerY, size / 2)
        }

        val newBed = CanvasBed(
            id = UUID.randomUUID().toString(),
            name = "",
            polygon = polygon,
            colorHex = BedColors.random()
        )

        executeAction(CanvasAction.AddBed(newBed))
        persistBed(newBed)

        _state.value = _state.value.copy(
            selectedBedId = newBed.id,
            mode = CanvasMode.BED_SELECTED,
            showBedLabelOverlay = true
        )
    }

    private fun createRectanglePolygon(cx: Float, cy: Float, width: Float, height: Float): CanvasPolygon {
        val halfW = width / 2
        val halfH = height / 2
        return CanvasPolygon(listOf(
            CanvasPoint(cx - halfW, cy - halfH),
            CanvasPoint(cx + halfW, cy - halfH),
            CanvasPoint(cx + halfW, cy + halfH),
            CanvasPoint(cx - halfW, cy + halfH)
        ))
    }

    private fun createCirclePolygon(cx: Float, cy: Float, radius: Float, segments: Int = 24): CanvasPolygon {
        val points = (0 until segments).map { i ->
            val angle = 2 * PI * i / segments
            CanvasPoint(
                (cx + radius * cos(angle)).toFloat(),
                (cy + radius * sin(angle)).toFloat()
            )
        }
        return CanvasPolygon(points)
    }

    // ==================== BED EDITING ====================

    fun showBedLabelOverlay() {
        if (_state.value.selectedBedId != null) {
            _state.value = _state.value.copy(showBedLabelOverlay = true)
        }
    }

    fun hideBedLabelOverlay() {
        _state.value = _state.value.copy(showBedLabelOverlay = false)
    }

    fun updateBedLabel(name: String, emoji: String?) {
        val bedId = _state.value.selectedBedId ?: return
        val oldBed = _state.value.beds.find { it.id == bedId } ?: return
        val newBed = oldBed.copy(name = name, emoji = emoji)

        executeAction(CanvasAction.UpdateBed(oldBed, newBed))

        viewModelScope.launch {
            bedToDomain(newBed)?.let { updateBedUseCase(it) }
        }

        _state.value = _state.value.copy(showBedLabelOverlay = false)
    }

    fun deleteBed(bedId: String) {
        val bed = _state.value.beds.find { it.id == bedId } ?: return

        executeAction(CanvasAction.DeleteBed(bed))

        viewModelScope.launch {
            deleteBedUseCase(bedId)
        }

        _state.value = _state.value.copy(
            selectedBedId = null,
            mode = CanvasMode.IDLE,
            showBedLabelOverlay = false
        )
    }

    // ==================== PLANT DRAGGING ====================

    fun startDraggingPlant(plantId: String, startPosition: CanvasPoint) {
        _state.value = _state.value.copy(
            mode = CanvasMode.DRAGGING_PLANT,
            draggedPlantId = plantId,
            draggedPlantPosition = startPosition
        )
    }

    private fun dropPlant() {
        val plantId = _state.value.draggedPlantId ?: return
        val position = _state.value.draggedPlantPosition ?: return
        val plant = _plants.value.find { it.id == plantId } ?: return

        val targetBed = _state.value.beds.find { it.contains(position) }

        if (targetBed != null) {
            val plantArea = PlantArea(
                plantId = plantId,
                plantName = plant.nameDE,
                plantEmoji = PlantEmojis.forPlant(plant.nameDE),
                position = position,
                radiusCm = plant.spacingInRowCm / 2
            )

            val newBed = targetBed.copy(plantAreas = targetBed.plantAreas + plantArea)
            executeAction(CanvasAction.AddPlant(targetBed.id, plantArea))
        }

        _state.value = _state.value.copy(
            mode = CanvasMode.IDLE,
            draggedPlantId = null,
            draggedPlantPosition = null
        )
    }

    // ==================== ZOOM & PAN ====================

    fun updateTransform(scale: Float, pan: Offset) {
        val newScale = (_state.value.scale * scale).coerceIn(0.3f, 4f)
        _state.value = _state.value.copy(
            scale = newScale,
            panOffset = _state.value.panOffset + pan / _state.value.scale
        )
    }

    fun resetTransform() {
        _state.value = _state.value.copy(
            scale = 1f,
            panOffset = Offset.Zero
        )
    }

    // ==================== UNDO / REDO ====================

    private fun executeAction(action: CanvasAction) {
        applyAction(action)
        undoStack.add(action)
        redoStack.clear()
        updateUndoRedoState()
    }

    private fun applyAction(action: CanvasAction) {
        _state.value = when (action) {
            is CanvasAction.AddBed -> _state.value.copy(
                beds = _state.value.beds + action.bed
            )
            is CanvasAction.UpdateBed -> _state.value.copy(
                beds = _state.value.beds.map { 
                    if (it.id == action.newBed.id) action.newBed else it 
                }
            )
            is CanvasAction.DeleteBed -> _state.value.copy(
                beds = _state.value.beds.filter { it.id != action.bed.id }
            )
            is CanvasAction.AddPlant -> _state.value.copy(
                beds = _state.value.beds.map { bed ->
                    if (bed.id == action.bedId) {
                        bed.copy(plantAreas = bed.plantAreas + action.plantArea)
                    } else bed
                }
            )
            is CanvasAction.RemovePlant -> _state.value.copy(
                beds = _state.value.beds.map { bed ->
                    if (bed.id == action.bedId) {
                        bed.copy(plantAreas = bed.plantAreas.filter { it.id != action.plantArea.id })
                    } else bed
                }
            )
        }
    }

    private fun reverseAction(action: CanvasAction) {
        _state.value = when (action) {
            is CanvasAction.AddBed -> _state.value.copy(
                beds = _state.value.beds.filter { it.id != action.bed.id }
            )
            is CanvasAction.UpdateBed -> _state.value.copy(
                beds = _state.value.beds.map { 
                    if (it.id == action.oldBed.id) action.oldBed else it 
                }
            )
            is CanvasAction.DeleteBed -> _state.value.copy(
                beds = _state.value.beds + action.bed
            )
            is CanvasAction.AddPlant -> _state.value.copy(
                beds = _state.value.beds.map { bed ->
                    if (bed.id == action.bedId) {
                        bed.copy(plantAreas = bed.plantAreas.filter { it.id != action.plantArea.id })
                    } else bed
                }
            )
            is CanvasAction.RemovePlant -> _state.value.copy(
                beds = _state.value.beds.map { bed ->
                    if (bed.id == action.bedId) {
                        bed.copy(plantAreas = bed.plantAreas + action.plantArea)
                    } else bed
                }
            )
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val action = undoStack.removeLast()
            reverseAction(action)
            redoStack.add(action)
            updateUndoRedoState()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val action = redoStack.removeLast()
            applyAction(action)
            undoStack.add(action)
            updateUndoRedoState()
        }
    }

    private fun updateUndoRedoState() {
        _state.value = _state.value.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    // ==================== PERSISTENCE ====================

    private fun persistBed(bed: CanvasBed) {
        viewModelScope.launch {
            val bounds = bed.bounds
            createBedUseCase(
                gardenId = _state.value.gardenId,
                name = bed.name.ifEmpty { "Beet" },
                positionX = canvasToCm(bounds.left),
                positionY = canvasToCm(bounds.top),
                widthCm = canvasToCm(bounds.width).coerceAtLeast(10),
                heightCm = canvasToCm(bounds.height).coerceAtLeast(10),
                colorHex = bed.colorHex
            )
        }
    }

    // ==================== COORDINATE CONVERSION ====================

    private fun canvasToCm(canvasValue: Float): Int {
        val gardenWidth = _state.value.gardenWidthCm
        return ((canvasValue / canvasWidth) * gardenWidth).toInt()
    }

    private fun cmToCanvas(cmValue: Int): Float {
        val gardenWidth = _state.value.gardenWidthCm
        return (cmValue.toFloat() / gardenWidth) * canvasWidth
    }

    private fun Bed.toCanvasBed(): CanvasBed {
        val left = cmToCanvas(positionX)
        val top = cmToCanvas(positionY)
        val right = cmToCanvas(positionX + widthCm)
        val bottom = cmToCanvas(positionY + heightCm)

        val points = listOf(
            CanvasPoint(left, top),
            CanvasPoint(right, top),
            CanvasPoint(right, bottom),
            CanvasPoint(left, bottom)
        )

        return CanvasBed(
            id = id,
            name = name,
            polygon = CanvasPolygon(points),
            colorHex = colorHex
        )
    }

    private fun bedToDomain(canvasBed: CanvasBed): Bed? {
        val gardenId = _state.value.gardenId
        if (gardenId.isEmpty()) return null

        val bounds = canvasBed.bounds
        return Bed(
            id = canvasBed.id,
            gardenId = gardenId,
            name = canvasBed.name.ifEmpty { "Beet" },
            positionX = canvasToCm(bounds.left),
            positionY = canvasToCm(bounds.top),
            widthCm = canvasToCm(bounds.width).coerceAtLeast(10),
            heightCm = canvasToCm(bounds.height).coerceAtLeast(10),
            colorHex = canvasBed.colorHex
        )
    }
}
