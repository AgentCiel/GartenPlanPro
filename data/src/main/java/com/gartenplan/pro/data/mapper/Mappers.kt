package com.gartenplan.pro.data.mapper

import com.gartenplan.pro.data.local.entity.*
import com.gartenplan.pro.domain.model.*
import com.gartenplan.pro.domain.model.CompostStatus as DomainCompostStatus
import com.gartenplan.pro.domain.model.CultureType as DomainCultureType
import com.gartenplan.pro.domain.model.PlantingStatus as DomainPlantingStatus

/**
 * Mappers to convert between Entity and Domain models
 */

// ==================== PLANT ====================

fun PlantEntity.toDomain(): Plant = Plant(
    id = id,
    nameDE = nameDE,
    nameEN = nameEN,
    latinName = latinName,
    category = category,
    sowIndoorStart = sowIndoorStart,
    sowIndoorEnd = sowIndoorEnd,
    sowOutdoorStart = sowOutdoorStart,
    sowOutdoorEnd = sowOutdoorEnd,
    harvestStart = harvestStart,
    harvestEnd = harvestEnd,
    daysToGermination = daysToGermination,
    daysToHarvest = daysToHarvest,
    spacingInRowCm = spacingInRowCm,
    spacingBetweenRowsCm = spacingBetweenRowsCm,
    plantDepthCm = plantDepthCm,
    nutrientDemand = nutrientDemand,
    waterDemand = waterDemand,
    sunRequirement = sunRequirement,
    description = description,
    careTips = careTips,
    diseases = diseases?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
    pests = pests?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
    imageUrl = imageUrl,
    isFavorite = isFavorite,
    isProOnly = isProOnly
)

fun Plant.toEntity(): PlantEntity = PlantEntity(
    id = id,
    nameDE = nameDE,
    nameEN = nameEN,
    latinName = latinName,
    category = category,
    sowIndoorStart = sowIndoorStart,
    sowIndoorEnd = sowIndoorEnd,
    sowOutdoorStart = sowOutdoorStart,
    sowOutdoorEnd = sowOutdoorEnd,
    harvestStart = harvestStart,
    harvestEnd = harvestEnd,
    daysToGermination = daysToGermination,
    daysToHarvest = daysToHarvest,
    spacingInRowCm = spacingInRowCm,
    spacingBetweenRowsCm = spacingBetweenRowsCm,
    plantDepthCm = plantDepthCm,
    nutrientDemand = nutrientDemand,
    waterDemand = waterDemand,
    sunRequirement = sunRequirement,
    description = description,
    careTips = careTips,
    diseases = diseases.joinToString(","),
    pests = pests.joinToString(","),
    imageUrl = imageUrl,
    isFavorite = isFavorite,
    isProOnly = isProOnly
)

fun List<PlantEntity>.toDomainList(): List<Plant> = map { it.toDomain() }

// ==================== GARDEN ====================

fun GardenEntity.toDomain(beds: List<Bed> = emptyList()): Garden = Garden(
    id = id,
    name = name,
    widthCm = widthCm,
    heightCm = heightCm,
    climateZone = climateZone,
    postalCode = postalCode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    imageUri = imageUri,
    notes = notes,
    beds = beds
)

fun Garden.toEntity(): GardenEntity = GardenEntity(
    id = id,
    name = name,
    widthCm = widthCm,
    heightCm = heightCm,
    climateZone = climateZone,
    postalCode = postalCode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    imageUri = imageUri,
    notes = notes
)

// ==================== BED ====================

fun BedEntity.toDomain(placements: List<PlantPlacement> = emptyList()): Bed = Bed(
    id = id,
    gardenId = gardenId,
    name = name,
    positionX = positionX,
    positionY = positionY,
    widthCm = widthCm,
    heightCm = heightCm,
    shape = shape,
    secondaryWidthCm = secondaryWidthCm,
    secondaryHeightCm = secondaryHeightCm,
    raisedHeightCm = raisedHeightCm,
    isPath = isPath,
    colorHex = colorHex,
    soilType = soilType,
    notes = notes,
    placements = placements
)

fun Bed.toEntity(): BedEntity = BedEntity(
    id = id,
    gardenId = gardenId,
    name = name,
    positionX = positionX,
    positionY = positionY,
    widthCm = widthCm,
    heightCm = heightCm,
    shape = shape,
    secondaryWidthCm = secondaryWidthCm,
    secondaryHeightCm = secondaryHeightCm,
    raisedHeightCm = raisedHeightCm,
    isPath = isPath,
    colorHex = colorHex,
    soilType = soilType,
    notes = notes
)

// ==================== PLANT PLACEMENT ====================

fun PlantPlacementEntity.toDomain(plant: Plant): PlantPlacement = PlantPlacement(
    id = id,
    bedId = bedId,
    plant = plant,
    positionX = positionX,
    positionY = positionY,
    widthCm = widthCm,
    heightCm = heightCm,
    quantity = quantity,
    year = year,
    cultureType = cultureType.toDomainCultureType(),
    sowDate = sowDate,
    transplantDate = transplantDate,
    harvestDate = harvestDate,
    status = status.toDomainPlantingStatus(),
    notes = notes
)

fun PlantPlacement.toEntity(): PlantPlacementEntity = PlantPlacementEntity(
    id = id,
    bedId = bedId,
    plantId = plant.id,
    positionX = positionX,
    positionY = positionY,
    widthCm = widthCm,
    heightCm = heightCm,
    quantity = quantity,
    year = year,
    cultureType = cultureType.toEntityCultureType(),
    sowDate = sowDate,
    transplantDate = transplantDate,
    harvestDate = harvestDate,
    status = status.toEntityPlantingStatus(),
    notes = notes
)

// Culture Type mapping
fun CultureType.toDomainCultureType(): DomainCultureType = when (this) {
    CultureType.PRE -> DomainCultureType.PRE
    CultureType.MAIN -> DomainCultureType.MAIN
    CultureType.POST -> DomainCultureType.POST
}

fun DomainCultureType.toEntityCultureType(): CultureType = when (this) {
    DomainCultureType.PRE -> CultureType.PRE
    DomainCultureType.MAIN -> CultureType.MAIN
    DomainCultureType.POST -> CultureType.POST
}

// Planting Status mapping
fun PlantingStatus.toDomainPlantingStatus(): DomainPlantingStatus = when (this) {
    PlantingStatus.PLANNED -> DomainPlantingStatus.PLANNED
    PlantingStatus.SOWN -> DomainPlantingStatus.SOWN
    PlantingStatus.TRANSPLANTED -> DomainPlantingStatus.TRANSPLANTED
    PlantingStatus.GROWING -> DomainPlantingStatus.GROWING
    PlantingStatus.HARVESTING -> DomainPlantingStatus.HARVESTING
    PlantingStatus.HARVESTED -> DomainPlantingStatus.HARVESTED
    PlantingStatus.FAILED -> DomainPlantingStatus.FAILED
}

fun DomainPlantingStatus.toEntityPlantingStatus(): PlantingStatus = when (this) {
    DomainPlantingStatus.PLANNED -> PlantingStatus.PLANNED
    DomainPlantingStatus.SOWN -> PlantingStatus.SOWN
    DomainPlantingStatus.TRANSPLANTED -> PlantingStatus.TRANSPLANTED
    DomainPlantingStatus.GROWING -> PlantingStatus.GROWING
    DomainPlantingStatus.HARVESTING -> PlantingStatus.HARVESTING
    DomainPlantingStatus.HARVESTED -> PlantingStatus.HARVESTED
    DomainPlantingStatus.FAILED -> PlantingStatus.FAILED
}

// ==================== TASK ====================

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    gardenId = gardenId,
    bedId = bedId,
    plantId = plantId,
    title = title,
    description = description,
    taskType = taskType,
    dueDate = dueDate,
    reminderTime = reminderTime,
    isCompleted = isCompleted,
    completedAt = completedAt,
    isAutoGenerated = isAutoGenerated,
    priority = priority,
    isRecurring = isRecurring,
    recurringDays = recurringDays
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    gardenId = gardenId,
    bedId = bedId,
    plantId = plantId,
    title = title,
    description = description,
    taskType = taskType,
    dueDate = dueDate,
    reminderTime = reminderTime,
    isCompleted = isCompleted,
    completedAt = completedAt,
    isAutoGenerated = isAutoGenerated,
    priority = priority,
    isRecurring = isRecurring,
    recurringDays = recurringDays
)

fun List<TaskEntity>.toTaskDomainList(): List<Task> = map { it.toDomain() }

// ==================== COMPOST ====================

fun CompostEntity.toDomain(
    entries: List<CompostEntry> = emptyList(),
    greenRatio: Float = 0f,
    brownRatio: Float = 0f
): Compost = Compost(
    id = id,
    name = name,
    startedAt = startedAt,
    expectedReadyAt = expectedReadyAt,
    status = status.toDomainCompostStatus(),
    notes = notes,
    entries = entries,
    greenRatio = greenRatio,
    brownRatio = brownRatio
)

fun Compost.toEntity(): CompostEntity = CompostEntity(
    id = id,
    name = name,
    startedAt = startedAt,
    expectedReadyAt = expectedReadyAt,
    status = status.toEntityCompostStatus(),
    notes = notes
)

fun CompostStatus.toDomainCompostStatus(): DomainCompostStatus = when (this) {
    CompostStatus.ACTIVE -> DomainCompostStatus.ACTIVE
    CompostStatus.RESTING -> DomainCompostStatus.RESTING
    CompostStatus.READY -> DomainCompostStatus.READY
    CompostStatus.USED -> DomainCompostStatus.USED
}

fun DomainCompostStatus.toEntityCompostStatus(): CompostStatus = when (this) {
    DomainCompostStatus.ACTIVE -> CompostStatus.ACTIVE
    DomainCompostStatus.RESTING -> CompostStatus.RESTING
    DomainCompostStatus.READY -> CompostStatus.READY
    DomainCompostStatus.USED -> CompostStatus.USED
}

fun CompostEntryEntity.toDomain(): CompostEntry = CompostEntry(
    id = id,
    compostId = compostId,
    materialType = materialType,
    customMaterialName = customMaterialName,
    isGreen = isGreen,
    amountLiters = amountLiters,
    addedAt = addedAt,
    notes = notes
)

fun CompostEntry.toEntity(): CompostEntryEntity = CompostEntryEntity(
    id = id,
    compostId = compostId,
    materialType = materialType,
    customMaterialName = customMaterialName,
    isGreen = isGreen,
    amountLiters = amountLiters,
    addedAt = addedAt,
    notes = notes
)
