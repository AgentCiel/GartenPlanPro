package com.gartenplan.pro.feature.garden.canvas

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenCanvasScreen(
    gardenId: String?,
    gardenName: String? = null,
    gardenWidthCm: Int? = null,
    gardenHeightCm: Int? = null,
    onNavigateBack: () -> Unit,
    viewModel: GardenCanvasViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showRulers by remember { mutableStateOf(true) }

    LaunchedEffect(gardenId, gardenName) {
        if (gardenId != null) {
            viewModel.loadGarden(gardenId)
        } else if (gardenName != null && gardenWidthCm != null && gardenHeightCm != null) {
            viewModel.createNewGarden(gardenName, gardenWidthCm, gardenHeightCm)
        }
    }

    Scaffold(
        topBar = {
            CanvasTopBar(
                gardenName = state.gardenName,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                showRulers = showRulers,
                onBack = onNavigateBack,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleRulers = { showRulers = !showRulers }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main content with optional rulers
            Row(modifier = Modifier.fillMaxSize()) {
                // Vertical ruler (left)
                if (showRulers) {
                    VerticalRuler(
                        heightPx = canvasSize.height.toFloat(),
                        gardenHeightCm = state.gardenHeightCm,
                        scale = state.scale,
                        panOffset = state.panOffset.y,
                        modifier = Modifier
                            .width(32.dp)
                            .fillMaxHeight()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Horizontal ruler (top)
                    if (showRulers) {
                        HorizontalRuler(
                            widthPx = canvasSize.width.toFloat(),
                            gardenWidthCm = state.gardenWidthCm,
                            scale = state.scale,
                            panOffset = state.panOffset.x,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        )
                    }

                    // Canvas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onSizeChanged { size ->
                                canvasSize = size
                                viewModel.setCanvasSize(size.width.toFloat(), size.height.toFloat())
                            }
                    ) {
                        GardenDrawingCanvas(
                            state = state,
                            onTouchStart = { viewModel.onTouchStart(it) },
                            onTouchMove = { viewModel.onTouchMove(it) },
                            onTouchEnd = { viewModel.onTouchEnd() },
                            onTransform = { scale, pan -> viewModel.updateTransform(scale, pan) },
                            onBedTap = { viewModel.selectBed(it) },
                            onCanvasTap = { viewModel.deselectBed() },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Size indicator while drawing
                        if (state.mode == CanvasMode.DRAWING_BED && state.currentDrawingPoints.size >= 2) {
                            DrawingSizeIndicator(
                                points = state.currentDrawingPoints,
                                gardenWidthCm = state.gardenWidthCm,
                                canvasWidth = canvasSize.width.toFloat(),
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // Mode indicator badge (top center)
            ModeBadge(
                mode = state.mode,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (showRulers) 32.dp else 8.dp)
            )

            // Bed Label Overlay
            AnimatedVisibility(
                visible = state.showBedLabelOverlay && state.selectedBed != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                state.selectedBed?.let { bed ->
                    BedLabelOverlay(
                        currentName = bed.name,
                        currentEmoji = bed.emoji,
                        onSave = { name, emoji -> viewModel.updateBedLabel(name, emoji) },
                        onDismiss = { viewModel.hideBedLabelOverlay() },
                        onDelete = { viewModel.deleteBed(bed.id) }
                    )
                }
            }

            // Bottom: Plant picker + Mode controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Plant picker (when bed selected in view mode)
                AnimatedVisibility(
                    visible = state.selectedBedId != null && 
                              state.mode == CanvasMode.IDLE && 
                              !state.showBedLabelOverlay,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    PlantPickerBar(
                        plants = plants.take(30),
                        selectedPlantId = state.draggedPlantId,
                        onPlantDragStart = { plantId, offset ->
                            viewModel.startDraggingPlant(plantId, CanvasPoint(offset.x, offset.y))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Bottom control bar
                CanvasControlBar(
                    mode = state.mode,
                    hasSelection = state.selectedBedId != null,
                    onToggleDrawMode = { viewModel.toggleDrawMode() },
                    onAddRectangle = { viewModel.addQuickBed(QuickBedShape.RECTANGLE) },
                    onAddCircle = { viewModel.addQuickBed(QuickBedShape.CIRCLE) },
                    onEditLabel = { viewModel.showBedLabelOverlay() },
                    onDeselect = { viewModel.deselectBed() },
                    onResetView = { viewModel.resetTransform() },
                    showResetView = state.scale != 1f || state.panOffset != Offset.Zero,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==================== TOP BAR ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanvasTopBar(
    gardenName: String,
    canUndo: Boolean,
    canRedo: Boolean,
    showRulers: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleRulers: () -> Unit
) {
    TopAppBar(
        title = { Text(gardenName.ifEmpty { "Neuer Garten" }, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
            }
        },
        actions = {
            // Ruler toggle
            IconButton(onClick = onToggleRulers) {
                Icon(
                    Icons.Default.Straighten,
                    "Lineal",
                    tint = if (showRulers) MaterialTheme.colorScheme.primary
                           else LocalContentColor.current.copy(alpha = 0.5f)
                )
            }
            // Undo
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    "Rückgängig",
                    tint = if (canUndo) LocalContentColor.current 
                           else LocalContentColor.current.copy(alpha = 0.3f)
                )
            }
            // Redo
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    "Wiederholen",
                    tint = if (canRedo) LocalContentColor.current 
                           else LocalContentColor.current.copy(alpha = 0.3f)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ==================== MODE BADGE ====================

@Composable
private fun ModeBadge(
    mode: CanvasMode,
    modifier: Modifier = Modifier
) {
    val (icon, text, color) = when (mode) {
        CanvasMode.DRAWING_BED -> Triple(Icons.Default.Draw, "Zeichnen", MaterialTheme.colorScheme.primary)
        CanvasMode.DRAGGING_PLANT -> Triple(Icons.Default.Eco, "Pflanze platzieren", MaterialTheme.colorScheme.tertiary)
        CanvasMode.BED_SELECTED -> Triple(Icons.Default.TouchApp, "Beet ausgewählt", MaterialTheme.colorScheme.secondary)
        CanvasMode.IDLE -> Triple(Icons.Default.PanTool, "Ansicht", MaterialTheme.colorScheme.outline)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

// ==================== RULERS ====================

@Composable
private fun HorizontalRuler(
    widthPx: Float,
    gardenWidthCm: Int,
    scale: Float,
    panOffset: Float,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        val cmPerPx = gardenWidthCm / (widthPx * scale)
        val pxPerCm = 1f / cmPerPx
        
        // Determine tick interval based on zoom
        val tickInterval = when {
            pxPerCm * scale > 20 -> 10    // Every 10cm
            pxPerCm * scale > 5 -> 50     // Every 50cm
            else -> 100                    // Every 100cm (1m)
        }
        
        val startCm = ((-panOffset * scale) * cmPerPx).toInt()
        val endCm = ((widthPx - panOffset * scale) * cmPerPx).toInt()
        
        for (cm in (startCm / tickInterval * tickInterval)..(endCm + tickInterval) step tickInterval) {
            val x = (cm / cmPerPx + panOffset) * scale
            if (x < 0 || x > size.width) continue
            
            // Tick mark
            val tickHeight = if (cm % 100 == 0) 12f else 6f
            drawLine(
                color = Color.Gray,
                start = Offset(x, size.height - tickHeight),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            
            // Label (only at meters or every 50cm if zoomed)
            if (cm % 100 == 0 || (tickInterval <= 50 && cm % 50 == 0)) {
                val label = if (cm >= 100) "${cm / 100}m" else "${cm}cm"
                val textLayout = textMeasurer.measure(
                    label,
                    TextStyle(fontSize = 9.sp, color = Color.Gray)
                )
                drawText(
                    textLayout,
                    topLeft = Offset(x - textLayout.size.width / 2, 2f)
                )
            }
        }
        
        // Bottom line
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(0f, size.height - 1),
            end = Offset(size.width, size.height - 1),
            strokeWidth = 1f
        )
    }
}

@Composable
private fun VerticalRuler(
    heightPx: Float,
    gardenHeightCm: Int,
    scale: Float,
    panOffset: Float,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        val cmPerPx = gardenHeightCm / (heightPx * scale)
        val pxPerCm = 1f / cmPerPx
        
        val tickInterval = when {
            pxPerCm * scale > 20 -> 10
            pxPerCm * scale > 5 -> 50
            else -> 100
        }
        
        val startCm = ((-panOffset * scale) * cmPerPx).toInt()
        val endCm = ((heightPx - panOffset * scale) * cmPerPx).toInt()
        
        for (cm in (startCm / tickInterval * tickInterval)..(endCm + tickInterval) step tickInterval) {
            val y = (cm / cmPerPx + panOffset) * scale
            if (y < 0 || y > size.height) continue
            
            val tickWidth = if (cm % 100 == 0) 12f else 6f
            drawLine(
                color = Color.Gray,
                start = Offset(size.width - tickWidth, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            
            if (cm % 100 == 0) {
                val label = "${cm / 100}m"
                val textLayout = textMeasurer.measure(
                    label,
                    TextStyle(fontSize = 9.sp, color = Color.Gray)
                )
                // Rotate would be nice but keeping it simple
                drawText(
                    textLayout,
                    topLeft = Offset(2f, y - textLayout.size.height / 2)
                )
            }
        }
        
        // Right line
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(size.width - 1, 0f),
            end = Offset(size.width - 1, size.height),
            strokeWidth = 1f
        )
    }
}

// ==================== SIZE INDICATOR ====================

@Composable
private fun DrawingSizeIndicator(
    points: List<CanvasPoint>,
    gardenWidthCm: Int,
    canvasWidth: Float,
    modifier: Modifier = Modifier
) {
    val bounds = CanvasPolygon(points).bounds
    val cmPerPx = gardenWidthCm / canvasWidth
    val widthCm = (bounds.width * cmPerPx).toInt()
    val heightCm = (bounds.height * cmPerPx).toInt()
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.inverseSurface
    ) {
        Text(
            text = "${widthCm} × ${heightCm} cm",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}

// ==================== BOTTOM CONTROL BAR ====================

@Composable
private fun CanvasControlBar(
    mode: CanvasMode,
    hasSelection: Boolean,
    onToggleDrawMode: () -> Unit,
    onAddRectangle: () -> Unit,
    onAddCircle: () -> Unit,
    onEditLabel: () -> Unit,
    onDeselect: () -> Unit,
    onResetView: () -> Unit,
    showResetView: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                hasSelection && mode != CanvasMode.DRAWING_BED -> {
                    // Bed selected: show edit options
                    FilledTonalButton(onClick = onEditLabel) {
                        Icon(Icons.Default.Label, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Benennen")
                    }
                    
                    OutlinedButton(onClick = onDeselect) {
                        Text("Fertig")
                    }
                }
                mode == CanvasMode.DRAWING_BED -> {
                    // Drawing mode: show cancel
                    Text(
                        "Mit Finger zeichnen...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    OutlinedButton(onClick = onToggleDrawMode) {
                        Text("Abbrechen")
                    }
                }
                else -> {
                    // Normal mode: show tools
                    
                    // Reset view button (only if zoomed/panned)
                    if (showResetView) {
                        IconButton(onClick = onResetView) {
                            Icon(Icons.Default.CenterFocusStrong, "Ansicht zurücksetzen")
                        }
                    }
                    
                    // Quick shapes
                    FilledTonalButton(onClick = onAddRectangle) {
                        Icon(Icons.Default.CropSquare, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rechteck")
                    }
                    
                    FilledTonalButton(onClick = onAddCircle) {
                        Icon(Icons.Default.Circle, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kreis")
                    }
                    
                    // Draw mode toggle
                    Button(
                        onClick = onToggleDrawMode,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Draw, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Zeichnen")
                    }
                }
            }
        }
    }
}

// ==================== MAIN CANVAS ====================

@Composable
private fun GardenDrawingCanvas(
    state: GardenCanvasState,
    onTouchStart: (CanvasPoint) -> Unit,
    onTouchMove: (CanvasPoint) -> Unit,
    onTouchEnd: () -> Unit,
    onTransform: (Float, Offset) -> Unit,
    onBedTap: (String) -> Unit,
    onCanvasTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val isDrawMode = state.mode == CanvasMode.DRAWING_BED

    Box(
        modifier = modifier
            .background(Color(0xFFF1F8E9))
            .then(
                if (isDrawMode) {
                    // DRAW MODE: Only detect drag for drawing
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                onTouchStart(CanvasPoint.fromOffset(
                                    transformPoint(offset, state.scale, state.panOffset)
                                ))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                onTouchMove(CanvasPoint.fromOffset(
                                    transformPoint(change.position, state.scale, state.panOffset)
                                ))
                            },
                            onDragEnd = { onTouchEnd() }
                        )
                    }
                } else {
                    // VIEW MODE: Zoom/Pan + Tap
                    Modifier
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                onTransform(zoom, pan)
                            }
                        }
                        .pointerInput(state.beds) {
                            detectTapGestures { offset ->
                                val transformed = transformPoint(offset, state.scale, state.panOffset)
                                val tappedBed = state.beds.find { 
                                    it.contains(CanvasPoint.fromOffset(transformed)) 
                                }
                                if (tappedBed != null) {
                                    onBedTap(tappedBed.id)
                                } else {
                                    onCanvasTap()
                                }
                            }
                        }
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            withTransform({
                scale(state.scale, state.scale, Offset.Zero)
                translate(state.panOffset.x, state.panOffset.y)
            }) {
                // Grid
                drawGrid(state.gardenWidthCm, state.gardenHeightCm, size)

                // Beds
                state.beds.forEach { bed ->
                    drawBed(
                        bed = bed,
                        isSelected = bed.id == state.selectedBedId,
                        textMeasurer = textMeasurer
                    )
                }

                // Drawing preview
                if (state.currentDrawingPoints.isNotEmpty()) {
                    drawDrawingPreview(state.currentDrawingPoints)
                }

                // Dragged plant preview
                state.draggedPlantPosition?.let { pos ->
                    drawPlantPreview(pos)
                }
            }
        }

        // Draw mode overlay hint
        if (isDrawMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(0.dp)
                    )
            )
        }
    }
}

private fun transformPoint(offset: Offset, scale: Float, pan: Offset): Offset {
    return Offset(
        (offset.x / scale) - pan.x,
        (offset.y / scale) - pan.y
    )
}

private fun DrawScope.drawGrid(gardenWidthCm: Int, gardenHeightCm: Int, canvasSize: Size) {
    val gridSizeCm = 50 // 50cm grid
    val pxPerCm = canvasSize.width / gardenWidthCm
    val gridSizePx = gridSizeCm * pxPerCm
    
    val colorLight = Color(0xFF81C784).copy(alpha = 0.2f)
    val colorDark = Color(0xFF81C784).copy(alpha = 0.4f)

    // Vertical lines
    var x = 0f
    var cmX = 0
    while (x < canvasSize.width) {
        val color = if (cmX % 100 == 0) colorDark else colorLight
        drawLine(color, Offset(x, 0f), Offset(x, canvasSize.height), strokeWidth = 1f)
        x += gridSizePx
        cmX += gridSizeCm
    }

    // Horizontal lines
    var y = 0f
    var cmY = 0
    while (y < canvasSize.height) {
        val color = if (cmY % 100 == 0) colorDark else colorLight
        drawLine(color, Offset(0f, y), Offset(canvasSize.width, y), strokeWidth = 1f)
        y += gridSizePx
        cmY += gridSizeCm
    }
    
    // Garden border
    drawRect(
        color = Color(0xFF4CAF50),
        topLeft = Offset.Zero,
        size = Size(canvasSize.width, canvasSize.height),
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawBed(
    bed: CanvasBed,
    isSelected: Boolean,
    textMeasurer: TextMeasurer
) {
    val color = BedColors.parse(bed.colorHex)
    val polygon = bed.polygon
    
    if (polygon.points.isEmpty()) return

    val path = Path().apply {
        moveTo(polygon.points.first().x, polygon.points.first().y)
        polygon.points.drop(1).forEach { point -> lineTo(point.x, point.y) }
        close()
    }

    // Shadow for selected
    if (isSelected) {
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = 0.2f),
            style = Stroke(width = 12f)
        )
    }

    // Fill
    drawPath(
        path = path,
        color = color.copy(alpha = if (isSelected) 0.9f else 0.75f)
    )

    // Border
    drawPath(
        path = path,
        color = if (isSelected) Color.White else color.copy(alpha = 0.9f),
        style = Stroke(
            width = if (isSelected) 4f else 2f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Plant areas
    bed.plantAreas.forEach { drawPlantArea(it) }

    // Label
    val label = bed.displayName()
    if (label.isNotEmpty() && bed.bounds.width > 60 && bed.bounds.height > 40) {
        val textStyle = TextStyle(
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(1f, 1f), 3f)
        )
        val textLayoutResult = textMeasurer.measure(label, textStyle)
        
        drawText(
            textLayoutResult,
            topLeft = Offset(
                bed.center.x - textLayoutResult.size.width / 2,
                bed.center.y - textLayoutResult.size.height / 2
            )
        )
    }
}

private fun DrawScope.drawPlantArea(plantArea: PlantArea) {
    val position = plantArea.position.toOffset()
    val radius = plantArea.radiusCm.toFloat()

    drawCircle(
        color = Color(0xFF66BB6A),
        radius = radius,
        center = position
    )
    drawCircle(
        color = Color(0xFF2E7D32),
        radius = radius,
        center = position,
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawDrawingPreview(points: List<CanvasPoint>) {
    if (points.size < 2) return

    val bounds = CanvasPolygon(points).bounds
    
    // Ghost fill
    drawRoundRect(
        color = Color(0xFF4CAF50).copy(alpha = 0.3f),
        topLeft = Offset(bounds.left, bounds.top),
        size = Size(bounds.width, bounds.height),
        cornerRadius = CornerRadius(8f)
    )

    // Drawing path
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }

    drawPath(
        path = path,
        color = Color(0xFF4CAF50),
        style = Stroke(
            width = 4f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
        )
    )
}

private fun DrawScope.drawPlantPreview(position: CanvasPoint) {
    drawCircle(
        color = Color(0xFF4CAF50).copy(alpha = 0.4f),
        radius = 25f,
        center = position.toOffset()
    )
    drawCircle(
        color = Color(0xFF4CAF50),
        radius = 25f,
        center = position.toOffset(),
        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
    )
}
