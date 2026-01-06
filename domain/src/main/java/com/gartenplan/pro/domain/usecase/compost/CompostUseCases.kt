package com.gartenplan.pro.domain.usecase.compost

import com.gartenplan.pro.core.constants.CompostMaterialType
import com.gartenplan.pro.domain.model.Compost
import com.gartenplan.pro.domain.model.CompostEntry
import com.gartenplan.pro.domain.model.CompostStatus
import com.gartenplan.pro.domain.repository.CompostRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Get all composts
 */
class GetAllCompostsUseCase @Inject constructor(
    private val repository: CompostRepository
) {
    operator fun invoke(): Flow<List<Compost>> = repository.getAllComposts()
}

/**
 * Get active composts
 */
class GetActiveCompostsUseCase @Inject constructor(
    private val repository: CompostRepository
) {
    operator fun invoke(): Flow<List<Compost>> = repository.getActiveComposts()
}

/**
 * Observe a single compost with entries
 */
class ObserveCompostUseCase @Inject constructor(
    private val repository: CompostRepository
) {
    operator fun invoke(compostId: String): Flow<Compost?> = 
        repository.observeCompostById(compostId)
}

/**
 * Create a new compost pile
 */
class CreateCompostUseCase @Inject constructor(
    private val repository: CompostRepository
) {
    suspend operator fun invoke(name: String, notes: String? = null): String {
        val compost = Compost(
            id = UUID.randomUUID().toString(),
            name = name,
            startedAt = System.currentTimeMillis(),
            expectedReadyAt = System.currentTimeMillis() + (180L * 24 * 60 * 60 * 1000), // ~6 months
            status = CompostStatus.ACTIVE,
            notes = notes
        )
        return repository.createCompost(compost)
    }
}

/**
 * Update compost
 */
class UpdateCompostUseCase @Inject constructor(
    private val repository: CompostRepository
) {
    suspend operator fun invoke(compost: Compost) {
        repository.updateCompost(compost)
    }
}

/**
 * Delete a compost
 */
class DeleteCompostUseCase @Inject constructor(
    private val repository: CompostRepository
) {
    suspend operator fun invoke(compostId: String) {
        repository.deleteCompost(compostId)
    }
}

/**
 * Update compost status
 */
class UpdateCompostStatusUseCase @Inject constructor(
    private val repository: CompostRepository
) {
    suspend operator fun invoke(compostId: String, status: CompostStatus) {
        repository.updateCompostStatus(compostId, status)
    }
}

/**
 * Add material to compost
 */
class AddCompostEntryUseCase @Inject constructor(
    private val repository: CompostRepository
) {
    suspend operator fun invoke(
        compostId: String,
        materialType: CompostMaterialType,
        amountLiters: Float? = null,
        notes: String? = null
    ): String {
        val entry = CompostEntry(
            id = UUID.randomUUID().toString(),
            compostId = compostId,
            materialType = materialType,
            isGreen = materialType.isGreen,
            amountLiters = amountLiters,
            addedAt = System.currentTimeMillis(),
            notes = notes
        )
        return repository.addEntry(entry)
    }
}

/**
 * Remove entry from compost
 */
class RemoveCompostEntryUseCase @Inject constructor(
    private val repository: CompostRepository
) {
    suspend operator fun invoke(entryId: String) {
        repository.deleteEntry(entryId)
    }
}

/**
 * Get green/brown ratio for a compost
 */
class GetCompostRatioUseCase @Inject constructor(
    private val repository: CompostRepository
) {
    data class CompostRatio(
        val greenRatio: Float,
        val brownRatio: Float,
        val isBalanced: Boolean,
        val recommendation: String
    )
    
    suspend operator fun invoke(compostId: String): CompostRatio {
        val (greenRatio, brownRatio) = repository.getGreenBrownRatio(compostId)
        
        // Ideal ratio is about 1:2 to 1:3 (green:brown)
        // So green should be about 25-33%, brown 67-75%
        val isBalanced = greenRatio in 0.2f..0.4f && brownRatio in 0.6f..0.8f
        
        val recommendation = when {
            greenRatio == 0f && brownRatio == 0f -> "Füge Material hinzu um zu starten"
            greenRatio > 0.5f -> "Zu viel Grünmaterial! Füge mehr Braunes hinzu (Laub, Karton, Stroh)"
            greenRatio < 0.15f -> "Zu viel Braunmaterial! Füge mehr Grünes hinzu (Küchenabfälle, Rasenschnitt)"
            isBalanced -> "Gutes Verhältnis! Weiter so."
            greenRatio > 0.4f -> "Etwas zu viel Grün. Füge etwas mehr Braunes hinzu."
            else -> "Etwas zu viel Braun. Füge etwas mehr Grünes hinzu."
        }
        
        return CompostRatio(
            greenRatio = greenRatio,
            brownRatio = brownRatio,
            isBalanced = isBalanced,
            recommendation = recommendation
        )
    }
}

/**
 * Calculate estimated readiness date
 */
class CalculateCompostReadinessUseCase @Inject constructor(
    private val repository: CompostRepository
) {
    data class ReadinessInfo(
        val startedAt: Long,
        val estimatedReadyAt: Long,
        val daysRemaining: Int,
        val percentComplete: Int,
        val status: CompostStatus
    )
    
    suspend operator fun invoke(compostId: String): ReadinessInfo? {
        val compost = repository.getCompostById(compostId) ?: return null
        
        val now = System.currentTimeMillis()
        val totalDuration = (compost.expectedReadyAt ?: (compost.startedAt + 180L * 24 * 60 * 60 * 1000)) - compost.startedAt
        val elapsed = now - compost.startedAt
        
        val percentComplete = ((elapsed.toFloat() / totalDuration) * 100).toInt().coerceIn(0, 100)
        val daysRemaining = ((compost.expectedReadyAt ?: now) - now) / (24 * 60 * 60 * 1000)
        
        return ReadinessInfo(
            startedAt = compost.startedAt,
            estimatedReadyAt = compost.expectedReadyAt ?: (compost.startedAt + 180L * 24 * 60 * 60 * 1000),
            daysRemaining = daysRemaining.toInt().coerceAtLeast(0),
            percentComplete = percentComplete,
            status = compost.status
        )
    }
}
