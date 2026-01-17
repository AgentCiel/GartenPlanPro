package com.gartenplan.pro.feature.moon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.floor

/**
 * Mondphasen
 */
enum class MoonPhase(
    val emoji: String,
    val nameDE: String,
    val description: String,
    val recommendation: String
) {
    NEW_MOON(
        emoji = "\uD83C\uDF11",
        nameDE = "Neumond",
        description = "Ruhephase - der Mond ist nicht sichtbar.",
        recommendation = "Gute Zeit für Bodenarbeiten und Planung. Pflanzen ruhen."
    ),
    WAXING_CRESCENT(
        emoji = "\uD83C\uDF12",
        nameDE = "Zunehmende Sichel",
        description = "Der Mond nimmt zu und wird sichtbarer.",
        recommendation = "Günstig für Blattpflanzen und oberirdisches Wachstum."
    ),
    FIRST_QUARTER(
        emoji = "\uD83C\uDF13",
        nameDE = "Erstes Viertel",
        description = "Halb beleuchteter, zunehmender Mond.",
        recommendation = "Gute Phase für Aussaat von Fruchtpflanzen."
    ),
    WAXING_GIBBOUS(
        emoji = "\uD83C\uDF14",
        nameDE = "Zunehmender Mond",
        description = "Fast voller, zunehmender Mond.",
        recommendation = "Ideale Zeit für Pflanzung und Umpflanzen."
    ),
    FULL_MOON(
        emoji = "\uD83C\uDF15",
        nameDE = "Vollmond",
        description = "Der Mond ist vollständig beleuchtet.",
        recommendation = "Höchste Energie. Günstig für Ernte und Blütenpflanzen."
    ),
    WANING_GIBBOUS(
        emoji = "\uD83C\uDF16",
        nameDE = "Abnehmender Mond",
        description = "Der Mond beginnt abzunehmen.",
        recommendation = "Zeit für Wurzelpflanzen und Bodenarbeiten."
    ),
    LAST_QUARTER(
        emoji = "\uD83C\uDF17",
        nameDE = "Letztes Viertel",
        description = "Halb beleuchteter, abnehmender Mond.",
        recommendation = "Günstig für Unkrautbekämpfung und Rückschnitt."
    ),
    WANING_CRESCENT(
        emoji = "\uD83C\uDF18",
        nameDE = "Abnehmende Sichel",
        description = "Der Mond nähert sich dem Neumond.",
        recommendation = "Ruhe vor dem neuen Zyklus. Kompostarbeiten."
    )
}

/**
 * Pflanzenkategorien für Mondempfehlungen
 */
enum class PlantMoonCategory(
    val nameDE: String,
    val emoji: String,
    val examples: String,
    val bestPhases: List<MoonPhase>
) {
    LEAF(
        nameDE = "Blattpflanzen",
        emoji = "\uD83E\uDD6C",
        examples = "Salat, Spinat, Kohl, Kräuter",
        bestPhases = listOf(MoonPhase.WAXING_CRESCENT, MoonPhase.FIRST_QUARTER)
    ),
    FRUIT(
        nameDE = "Fruchtpflanzen",
        emoji = "\uD83C\uDF45",
        examples = "Tomaten, Paprika, Kürbis, Gurken",
        bestPhases = listOf(MoonPhase.FIRST_QUARTER, MoonPhase.WAXING_GIBBOUS)
    ),
    ROOT(
        nameDE = "Wurzelpflanzen",
        emoji = "\uD83E\uDD55",
        examples = "Karotten, Rüben, Kartoffeln, Zwiebeln",
        bestPhases = listOf(MoonPhase.WANING_GIBBOUS, MoonPhase.LAST_QUARTER)
    ),
    FLOWER(
        nameDE = "Blütenpflanzen",
        emoji = "\uD83C\uDF3B",
        examples = "Sonnenblumen, Rosen, Lavendel",
        bestPhases = listOf(MoonPhase.WAXING_GIBBOUS, MoonPhase.FULL_MOON)
    )
}

/**
 * Berechnet die aktuelle Mondphase
 */
fun calculateMoonPhase(date: LocalDate = LocalDate.now()): MoonPhase {
    // Vereinfachte Mondphasen-Berechnung (Synodischer Monat ~29.53 Tage)
    val referenceNewMoon = LocalDate.of(2024, 1, 11) // Bekannter Neumond
    val daysSinceRef = date.toEpochDay() - referenceNewMoon.toEpochDay()
    val synodicMonth = 29.53
    val phaseDay = ((daysSinceRef % synodicMonth) + synodicMonth) % synodicMonth

    return when {
        phaseDay < 1.85 -> MoonPhase.NEW_MOON
        phaseDay < 7.38 -> MoonPhase.WAXING_CRESCENT
        phaseDay < 9.23 -> MoonPhase.FIRST_QUARTER
        phaseDay < 14.77 -> MoonPhase.WAXING_GIBBOUS
        phaseDay < 16.61 -> MoonPhase.FULL_MOON
        phaseDay < 22.15 -> MoonPhase.WANING_GIBBOUS
        phaseDay < 24.0 -> MoonPhase.LAST_QUARTER
        else -> MoonPhase.WANING_CRESCENT
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoonScreen() {
    val currentPhase = remember { calculateMoonPhase() }
    val today = remember { LocalDate.now() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mond & Garten") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Aktuelle Mondphase
            item {
                CurrentMoonPhaseCard(phase = currentPhase)
            }

            // Hinweis-Banner
            item {
                InfoBanner()
            }

            // Pflanzenkategorien
            item {
                Text(
                    "Empfehlungen nach Pflanzenart",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(PlantMoonCategory.entries) { category ->
                PlantCategoryCard(
                    category = category,
                    currentPhase = currentPhase
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun CurrentMoonPhaseCard(phase: MoonPhase) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mond-Emoji groß
            Text(
                text = phase.emoji,
                fontSize = 64.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = phase.nameDE,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = phase.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = phase.recommendation,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InfoBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Der Mondkalender dient als Orientierung. Viele Gärtner nutzen diese Empfehlungen, sie sind jedoch kein Muss.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun PlantCategoryCard(
    category: PlantMoonCategory,
    currentPhase: MoonPhase
) {
    val isGoodPhase = currentPhase in category.bestPhases

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            Text(
                text = category.emoji,
                fontSize = 32.sp
            )

            Spacer(Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.nameDE,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = category.examples,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isGoodPhase)
                    Color(0xFF4CAF50).copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (isGoodPhase) "Günstig" else "Neutral",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isGoodPhase)
                        Color(0xFF2E7D32)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
