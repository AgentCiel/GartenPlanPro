package com.gartenplan.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gartenplan.pro.core.constants.*
import com.gartenplan.pro.ui.theme.*

// ==================== COMPANION INDICATOR ====================

@Composable
fun CompanionIndicator(
    type: CompanionType,
    modifier: Modifier = Modifier,
    size: CompanionIndicatorSize = CompanionIndicatorSize.Medium
) {
    val color = when (type) {
        CompanionType.GOOD -> CompanionGood
        CompanionType.NEUTRAL -> CompanionNeutral
        CompanionType.BAD -> CompanionBad
    }
    
    val icon = when (type) {
        CompanionType.GOOD -> Icons.Default.ThumbUp
        CompanionType.NEUTRAL -> Icons.Default.HorizontalRule
        CompanionType.BAD -> Icons.Default.ThumbDown
    }
    
    val iconSize = when (size) {
        CompanionIndicatorSize.Small -> 12.dp
        CompanionIndicatorSize.Medium -> 16.dp
        CompanionIndicatorSize.Large -> 24.dp
    }
    
    val boxSize = when (size) {
        CompanionIndicatorSize.Small -> 20.dp
        CompanionIndicatorSize.Medium -> 28.dp
        CompanionIndicatorSize.Large -> 40.dp
    }
    
    Box(
        modifier = modifier
            .size(boxSize)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type.getDisplayName(),
            tint = color,
            modifier = Modifier.size(iconSize)
        )
    }
}

enum class CompanionIndicatorSize { Small, Medium, Large }

// ==================== NUTRIENT LEVEL BADGE ====================

@Composable
fun NutrientLevelBadge(
    level: NutrientLevel,
    modifier: Modifier = Modifier
) {
    val color = when (level) {
        NutrientLevel.HIGH -> NutrientHigh
        NutrientLevel.MEDIUM -> NutrientMedium
        NutrientLevel.LOW -> NutrientLow
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = when (level) {
                NutrientLevel.HIGH -> "S"
                NutrientLevel.MEDIUM -> "M"
                NutrientLevel.LOW -> "L"
            },
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ==================== SUN REQUIREMENT ICON ====================

@Composable
fun SunRequirementIcon(
    level: SunLevel,
    modifier: Modifier = Modifier
) {
    val icon = when (level) {
        SunLevel.FULL_SUN -> Icons.Default.WbSunny
        SunLevel.PARTIAL_SHADE -> Icons.Default.WbTwilight
        SunLevel.SHADE -> Icons.Default.Cloud
    }
    
    val color = when (level) {
        SunLevel.FULL_SUN -> Color(0xFFFFC107)
        SunLevel.PARTIAL_SHADE -> Color(0xFFFF9800)
        SunLevel.SHADE -> Color(0xFF9E9E9E)
    }
    
    Icon(
        imageVector = icon,
        contentDescription = level.getDisplayName(),
        tint = color,
        modifier = modifier.size(16.dp)
    )
}

// ==================== WATER LEVEL ICON ====================

@Composable
fun WaterLevelIcon(
    level: WaterLevel,
    modifier: Modifier = Modifier
) {
    val drops = when (level) {
        WaterLevel.HIGH -> 3
        WaterLevel.MEDIUM -> 2
        WaterLevel.LOW -> 1
    }
    
    Row(modifier = modifier) {
        repeat(drops) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = Teal60,
                modifier = Modifier.size(12.dp)
            )
        }
        repeat(3 - drops) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = Color.LightGray.copy(alpha = 0.3f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// ==================== MONTH SELECTOR ====================

@Composable
fun MonthSelector(
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val months = listOf(
        "Jan", "Feb", "Mär", "Apr", "Mai", "Jun",
        "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        months.forEachIndexed { index, month ->
            val monthNumber = index + 1
            val isSelected = monthNumber == selectedMonth
            
            FilterChip(
                selected = isSelected,
                onClick = { onMonthSelected(monthNumber) },
                label = { Text(month) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// ==================== LOADING / ERROR / EMPTY STATES ====================

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    message: String = "Laden..."
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Fehler",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onRetry) {
                    Text("Erneut versuchen")
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

// ==================== SEARCH BAR ====================

@Composable
fun GartenSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Suchen...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Suchen"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Löschen"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

// ==================== CATEGORY CHIPS ====================

@Composable
fun CategoryChips(
    selectedCategory: PlantCategory?,
    onCategorySelected: (PlantCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("Alle") }
        )
        
        PlantCategory.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.getDisplayName()) },
                leadingIcon = if (selectedCategory == category) {
                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}
