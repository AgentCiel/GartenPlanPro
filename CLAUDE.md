# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew assembleDebug

# Run all unit tests
./gradlew test

# Run a single test class
./gradlew :feature-garden:testDebugUnitTest --tests "com.gartenplan.pro.feature.garden.ExampleUnitTest"

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Clean and rebuild
./gradlew clean assembleDebug

# Check for dependency updates
./gradlew dependencyUpdates
```

## Architecture Overview

This is a **multi-module Android app** using Clean Architecture with Jetpack Compose UI. The app is a German garden planning tool ("GartenPlan Pro").

### Module Dependency Graph

```
app
├── core (constants, utilities)
├── data (Room database, repositories)
├── domain (models, use cases, repository interfaces)
├── ui-components (shared Compose components)
├── feature-garden
├── feature-plants
├── feature-calendar
└── feature-compost
```

### Layer Responsibilities

- **core**: App-wide constants (`Constants.kt`), enums (`PlantCategory`, `NutrientLevel`, `WaterLevel`, `SunLevel`, `CompanionType`, `BedShape`, `TaskType`, `CompostMaterialType`)
- **domain**: Pure Kotlin models (`DomainModels.kt`), repository interfaces (`Repositories.kt`), and use case classes (one class per operation, e.g., `CreateGardenUseCase`)
- **data**: Room database (`GartenPlanDatabase`), entities, DAOs, mappers, and repository implementations. Database is seeded on first launch via `DatabaseSeeder`
- **feature-***: Compose screens and ViewModels. Each feature depends on `core`, `domain`, and `ui-components`
- **app**: MainActivity, navigation (`GartenNavHost`, `Routes`), Hilt setup

### Key Patterns

- **Dependency Injection**: Hilt with `@HiltViewModel`, modules in `data/di/` (`DatabaseModule`, `RepositoryModule`)
- **State Management**: `StateFlow` in ViewModels, collected via `collectAsStateWithLifecycle()`
- **Navigation**: Jetpack Navigation Compose. Routes defined in `app/navigation/Navigation.kt`. Helper functions for parameterized routes (e.g., `Routes.gardenEditor(gardenId)`)
- **Database**: Room with KSP annotation processing. Entities map 1:1 with domain models via `Mappers.kt`
- **Use Cases**: Single-responsibility classes with `operator fun invoke()` pattern. Injected into ViewModels

### Domain Models

Primary entities in `domain/model/DomainModels.kt`:
- `Plant` - Plant data with sowing/harvest months, spacing, nutrient/water/sun levels
- `Garden` - Garden dimensions and metadata, contains list of `Bed`
- `Bed` - Positioned bed within a garden, contains `PlantPlacement`s
- `PlantPlacement` - A plant placed in a bed with position, quantity, year, status
- `Task` - Calendar tasks for garden maintenance
- `Compost` - Compost tracking with entries

### Garden Editor

The garden editor (`feature-garden/editor/`) uses a coordinate system where:
- Positions are stored in **centimeters** in the database
- The editor works in **meters** internally
- Conversion: `positionCm / 100f` to meters, `positionM * 100` to cm
- `GardenEditorViewModel` handles two modes: `BEWEGUNG` (navigation) and `BUILD` (editing beds)

### Localization

The app is bilingual (German/English). Models have dual name fields (e.g., `nameDE`/`nameEN`). Enums provide `getDisplayName(isGerman: Boolean)` methods.
