package com.gartenplan.pro.feature.garden.editor

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenEditorScreen(
    gardenId: String?,
    gardenName: String? = null,
    gardenWidthM: Float? = null,
    gardenHeightM: Float? = null,
    onNavigateBack: () -> Unit,
    onNavigateToBedDetail: (String) -> Unit,
    viewModel: GardenEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Laden
    LaunchedEffect(gardenId, gardenName) {
        if (gardenId != null) {
            viewModel.loadGarden(gardenId)
        } else if (gardenName != null && gardenWidthM != null && gardenHeightM != null) {
            viewModel.createNewGarden(gardenName, gardenWidthM, gardenHeightM)
        }
    }

    // Pixels per Meter berechnen
    LaunchedEffect(canvasSize, state.gardenWidthM) {
        if (canvasSize.width > 0 && state.gardenWidthM > 0) {
            val ppm = canvasSize.width / state.gardenWidthM
            viewModel.setPixelsPerMeter(ppm)
        }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                gardenName = state.gardenName,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                onBack = onNavigateBack,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Canvas
            EditorCanvas(
                state = state,
                onSizeChanged = { canvasSize = it },
                // Bewegungsmodus
                onNavPan = { viewModel.onNavigationPan(it) },
                onNavZoom = { viewModel.onNavigationZoom(it) },
                onNavTap = { viewModel.onNavigationTap(it) },
                // Build-Modus
                onBuildStart = { viewModel.onBuildTouchStart(it) },
                onBuildMove = { viewModel.onBuildTouchMove(it) },
                onBuildEnd = { viewModel.onBuildTouchEnd() },
                onBuildPanZoom = { pan, zoom -> viewModel.onBuildPanZoom(pan, zoom) },
                modifier = Modifier.fillMaxSize()
            )

            // Modus-Anzeige (oben)
            ModeIndicator(
                mode = state.mode,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            // Modus-Toggle (oben links)
            ModeToggle(
                mode = state.mode,
                onToggle = { viewModel.toggleMode() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )

            // Auswahl-Info (wenn Beet ausgewählt)
            AnimatedVisibility(
                visible = state.selectedBed != null && state.mode == EditorMode.BUILD,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                state.selectedBed?.let { bed ->
                    SelectedBedInfo(
                        bed = bed,
                        onOpenDetail = { onNavigateToBedDetail(bed.id) },
                        onDelete = { viewModel.deleteBed(bed.id) }
                    )
                }
            }

            // Toolbar (unten) - nur im Build-Modus
            AnimatedVisibility(
                visible = state.mode == EditorMode.BUILD,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BuildToolbar(
                    activeTool = state.tool,
                    isDrawing = state.isDrawing,
                    onToolSelected = { viewModel.setTool(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Zeichnungs-Preview Info
            if (state.isDrawing && state.drawPreviewRect != null) {
                val rect = state.drawPreviewRect!!
                DrawingInfo(
                    width = rect.width,
                    height = rect.height,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                )
            }
        }
    }
}

// ==================== TOP BAR ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    gardenName: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    TopAppBar(
        title = { Text(gardenName.ifEmpty { "Garten" }, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
            }
        },
        actions = {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo, "Rückgängig",
                    tint = if (canUndo) LocalContentColor.current 
                           else LocalContentColor.current.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo, "Wiederholen",
                    tint = if (canRedo) LocalContentColor.current 
                           else LocalContentColor.current.copy(alpha = 0.3f)
                )
            }
        }
    )
}

// ==================== MODE INDICATOR ====================

@Composable
private fun ModeIndicator(
    mode: EditorMode,
    modifier: Modifier = Modifier
) {
    val (icon, text, color) = when (mode) {
        EditorMode.BEWEGUNG -> Triple(
            Icons.Default.PanTool, 
            "Bewegungsmodus", 
            MaterialTheme.colorScheme.secondary
        )
        EditorMode.BUILD -> Triple(
            Icons.Default.Construction, 
            "Build-Modus", 
            MaterialTheme.colorScheme.primary
        )
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
            Icon(icon, null, Modifier.size(16.dp), tint = color)
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

// ==================== MODE TOGGLE ====================

@Composable
private fun ModeToggle(
    mode: EditorMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBuild = mode == EditorMode.BUILD
    
    FloatingActionButton(
        onClick = onToggle,
        modifier = modifier,
        containerColor = if (isBuild) MaterialTheme.colorScheme.primary 
                         else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (isBuild) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Icon(
            if (isBuild) Icons.Default.Construction else Icons.Default.PanTool,
            if (isBuild) "Zu Bewegung wechseln" else "Zu Build wechseln"
        )
    }
}

// ==================== SELECTED BED INFO ====================

@Composable
private fun SelectedBedInfo(
    bed: EditorBed,
    onOpenDetail: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                bed.displayName(),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                bed.sizeText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                bed.areaText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(8.dp))
            
            Row {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Löschen")
                }
                
                Button(onClick = onOpenDetail) {
                    Text("Planen")
                }
            }
        }
    }
}

// ==================== BUILD TOOLBAR ====================

@Composable
private fun BuildToolbar(
    activeTool: BuildTool,
    isDrawing: Boolean,
    onToolSelected: (BuildTool) -> Unit,
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
            ToolButton(
                icon = Icons.Default.TouchApp,
                label = "Auswählen",
                isActive = activeTool == BuildTool.SELECT,
                onClick = { onToolSelected(BuildTool.SELECT) }
            )
            
            ToolButton(
                icon = Icons.Default.CropSquare,
                label = "Beet",
                isActive = activeTool == BuildTool.ADD_BED,
                onClick = { onToolSelected(BuildTool.ADD_BED) }
            )
            
            ToolButton(
                icon = Icons.Default.LinearScale,
                label = "Weg",
                isActive = activeTool == BuildTool.ADD_PATH,
                onClick = { onToolSelected(BuildTool.ADD_PATH) }
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val color = if (isActive) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) color.copy(alpha = 0.1f) else Color.Transparent)
            .padding(8.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, label, tint = color)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

// ==================== DRAWING INFO ====================

@Composable
private fun DrawingInfo(
    width: Float,
    height: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.inverseSurface
    ) {
        Text(
            text = "%.1f × %.1f m  (%.1f m²)".format(width, height, width * height),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}

// ==================== CANVAS ====================

@Composable
private fun EditorCanvas(
    state: GardenEditorState,
    onSizeChanged: (IntSize) -> Unit,
    // Bewegungsmodus
    onNavPan: (Offset) -> Unit,
    onNavZoom: (Float) -> Unit,
    onNavTap: (Offset) -> Unit,
    // Build-Modus
    onBuildStart: (Offset) -> Unit,
    onBuildMove: (Offset) -> Unit,
    onBuildEnd: () -> Unit,
    onBuildPanZoom: (Offset, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    // Berechne Pixel pro Meter
    var pixelsPerMeter by remember { mutableFloatStateOf(100f) }
    
    Box(
        modifier = modifier
            .background(Color(0xFFF5F5F5))
            .onSizeChanged { size ->
                onSizeChanged(size)
                if (state.gardenWidthM > 0) {
                    pixelsPerMeter = size.width / state.gardenWidthM
                }
            }
            .then(
                when (state.mode) {
                    EditorMode.BEWEGUNG -> Modifier
                        // 1 Finger = Pan
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onNavPan(dragAmount)
                            }
                        }
                        // 2 Finger = Zoom
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                onNavZoom(zoom)
                            }
                        }
                        // Tap = Highlight
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                onNavTap(offset)
                            }
                        }
                    
                    EditorMode.BUILD -> Modifier
                        // Zeichnen / Drag / Resize
                        .pointerInput(state.tool, state.isDrawing, state.isDragging, state.isResizing) {
                            detectDragGestures(
                                onDragStart = { onBuildStart(it) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    onBuildMove(change.position)
                                },
                                onDragEnd = { onBuildEnd() },
                                onDragCancel = { onBuildEnd() }
                            )
                        }
                        // 2 Finger Zoom/Pan (nur wenn nichts aktiv)
                        .pointerInput(state.isDrawing, state.isDragging, state.isResizing) {
                            if (!state.isDrawing && !state.isDragging && !state.isResizing) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    onBuildPanZoom(pan, zoom)
                                }
                            }
                        }
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = state.scale
            val pan = state.panOffset
            
            withTransform({
                scale(scale, scale, Offset.Zero)
                translate(pan.x, pan.y)
            }) {
                // Gartenfläche
                drawGardenArea(state.gardenWidthM, state.gardenHeightM, pixelsPerMeter)
                
                // Grid
                drawGrid(state.gardenWidthM, state.gardenHeightM, pixelsPerMeter)
                
                // Wege
                state.paths.forEach { path ->
                    drawPath(path, pixelsPerMeter)
                }
                
                // Beete
                state.beds.forEach { bed ->
                    val isSelected = bed.id == state.selectedBedId
                    val isHighlighted = bed.id == state.highlightedBedId
                    drawBed(bed, isSelected, isHighlighted, pixelsPerMeter, textMeasurer)
                }
                
                // Zeichnungs-Preview
                state.drawPreviewRect?.let { rect ->
                    drawPreviewRect(rect, state.tool, pixelsPerMeter)
                }
            }
        }
        
        // Build-Modus Rahmen
        if (state.mode == EditorMode.BUILD) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            )
        }
    }
}

// ==================== DRAW FUNCTIONS ====================

private fun DrawScope.drawGardenArea(widthM: Float, heightM: Float, ppm: Float) {
    val widthPx = widthM * ppm
    val heightPx = heightM * ppm
    
    // Hintergrund
    drawRect(
        color = Color(0xFFE8F5E9),  // Hellgrün
        topLeft = Offset.Zero,
        size = Size(widthPx, heightPx)
    )
    
    // Rahmen
    drawRect(
        color = Color(0xFF4CAF50),
        topLeft = Offset.Zero,
        size = Size(widthPx, heightPx),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawGrid(widthM: Float, heightM: Float, ppm: Float) {
    val gridM = 1f  // 1 Meter Grid
    val gridPx = gridM * ppm
    
    val lightColor = Color(0xFF81C784).copy(alpha = 0.3f)
    val darkColor = Color(0xFF81C784).copy(alpha = 0.5f)
    
    // Vertikale Linien
    var x = 0f
    var meterX = 0
    while (x <= widthM * ppm) {
        val color = if (meterX % 5 == 0) darkColor else lightColor
        drawLine(color, Offset(x, 0f), Offset(x, heightM * ppm), strokeWidth = 1f)
        x += gridPx
        meterX++
    }
    
    // Horizontale Linien
    var y = 0f
    var meterY = 0
    while (y <= heightM * ppm) {
        val color = if (meterY % 5 == 0) darkColor else lightColor
        drawLine(color, Offset(0f, y), Offset(widthM * ppm, y), strokeWidth = 1f)
        y += gridPx
        meterY++
    }
}

private fun DrawScope.drawBed(
    bed: EditorBed,
    isSelected: Boolean,
    isHighlighted: Boolean,
    ppm: Float,
    textMeasurer: TextMeasurer
) {
    val left = bed.x * ppm
    val top = bed.y * ppm
    val width = bed.width * ppm
    val height = bed.height * ppm
    
    val color = BedColors.parse(bed.colorHex)
    
    // Schatten für ausgewählte
    if (isSelected) {
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.2f),
            topLeft = Offset(left + 4, top + 4),
            size = Size(width, height),
            cornerRadius = CornerRadius(8f)
        )
    }
    
    // Fläche
    drawRoundRect(
        color = color.copy(alpha = if (isSelected) 0.9f else if (isHighlighted) 0.85f else 0.75f),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(8f)
    )
    
    // Rahmen
    drawRoundRect(
        color = if (isSelected) Color.White else if (isHighlighted) Color.White.copy(alpha = 0.7f) else color,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(8f),
        style = Stroke(width = if (isSelected) 4f else 2f)
    )
    
    // Resize-Handles für ausgewählte
    if (isSelected) {
        val handleRadius = 10f
        val corners = listOf(
            Offset(left, top),
            Offset(left + width, top),
            Offset(left, top + height),
            Offset(left + width, top + height)
        )
        corners.forEach { corner ->
            drawCircle(Color.White, handleRadius, corner)
            drawCircle(color, handleRadius - 2, corner)
        }
    }
    
    // Label
    if (width > 60 && height > 40) {
        val label = bed.displayName()
        val sizeLabel = bed.areaText()
        
        val labelStyle = TextStyle(
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(1f, 1f), 2f)
        )
        val sizeStyle = TextStyle(
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(1f, 1f), 2f)
        )
        
        val labelResult = textMeasurer.measure(label, labelStyle)
        val sizeResult = textMeasurer.measure(sizeLabel, sizeStyle)
        
        val centerX = left + width / 2
        val centerY = top + height / 2
        
        drawText(labelResult, topLeft = Offset(
            centerX - labelResult.size.width / 2,
            centerY - labelResult.size.height
        ))
        drawText(sizeResult, topLeft = Offset(
            centerX - sizeResult.size.width / 2,
            centerY + 2
        ))
    }
}

private fun DrawScope.drawPath(path: EditorPath, ppm: Float) {
    val startPx = Offset(path.startX * ppm, path.startY * ppm)
    val endPx = Offset(path.endX * ppm, path.endY * ppm)
    val widthPx = path.pathWidth * ppm
    
    drawLine(
        color = PathColor.default,
        start = startPx,
        end = endPx,
        strokeWidth = widthPx,
        cap = StrokeCap.Round
    )
    
    // Rahmen
    drawLine(
        color = Color(0xFF757575),
        start = startPx,
        end = endPx,
        strokeWidth = widthPx,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
    )
}

private fun DrawScope.drawPreviewRect(rect: androidx.compose.ui.geometry.Rect, tool: BuildTool, ppm: Float) {
    val left = rect.left * ppm
    val top = rect.top * ppm
    val width = rect.width * ppm
    val height = rect.height * ppm
    
    when (tool) {
        BuildTool.ADD_BED -> {
            // Beet-Preview
            drawRoundRect(
                color = Color(0xFF4CAF50).copy(alpha = 0.3f),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(8f)
            )
            drawRoundRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(8f),
                style = Stroke(
                    width = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
                )
            )
        }
        BuildTool.ADD_PATH -> {
            // Weg-Preview (Linie)
            drawLine(
                color = PathColor.default.copy(alpha = 0.5f),
                start = Offset(left, top),
                end = Offset(left + width, top + height),
                strokeWidth = 0.4f * ppm,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
            )
        }
        else -> {}
    }
}
