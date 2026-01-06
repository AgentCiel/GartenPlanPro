package com.gartenplan.pro.core.constants

/**
 * App-wide constants
 */
object Constants {
    // App Info
    const val APP_NAME = "GartenPlan Pro"
    const val APP_VERSION = "1.0.0"

    // Database
    const val DATABASE_NAME = "gartenplan_database"
    const val DATABASE_VERSION = 1

    // Free Version Limits
    const val FREE_MAX_GARDENS = 1
    const val FREE_MAX_PLANTS = 50

    // Default Values
    const val DEFAULT_BED_WIDTH_CM = 120
    const val DEFAULT_BED_HEIGHT_CM = 200
    const val DEFAULT_PATH_WIDTH_CM = 50

    // Grid
    const val GRID_CELL_SIZE_CM = 10

    // Climate Zones (Germany)
    const val DEFAULT_CLIMATE_ZONE = "7a"
}

/**
 * Plant categories
 */
enum class PlantCategory(val displayNameDE: String, val displayNameEN: String) {
    VEGETABLE("Gemüse", "Vegetable"),
    FRUIT("Obst", "Fruit"),
    HERB("Kräuter", "Herb"),
    FLOWER("Blumen", "Flower"),
    GREEN_MANURE("Gründüngung", "Green Manure");

    fun getDisplayName(isGerman: Boolean = true): String {
        return if (isGerman) displayNameDE else displayNameEN
    }
}

/**
 * Nutrient demand levels (Stark-/Mittel-/Schwachzehrer)
 */
enum class NutrientLevel(val displayNameDE: String, val displayNameEN: String, val value: Int) {
    HIGH("Starkzehrer", "Heavy Feeder", 3),
    MEDIUM("Mittelzehrer", "Medium Feeder", 2),
    LOW("Schwachzehrer", "Light Feeder", 1);

    fun getDisplayName(isGerman: Boolean = true): String {
        return if (isGerman) displayNameDE else displayNameEN
    }
}

/**
 * Water demand levels
 */
enum class WaterLevel(val displayNameDE: String, val displayNameEN: String, val value: Int) {
    HIGH("Hoch", "High", 3),
    MEDIUM("Mittel", "Medium", 2),
    LOW("Niedrig", "Low", 1);

    fun getDisplayName(isGerman: Boolean = true): String {
        return if (isGerman) displayNameDE else displayNameEN
    }
}

/**
 * Sun requirement levels
 */
enum class SunLevel(val displayNameDE: String, val displayNameEN: String, val hoursMin: Int) {
    FULL_SUN("Volle Sonne", "Full Sun", 6),
    PARTIAL_SHADE("Halbschatten", "Partial Shade", 3),
    SHADE("Schatten", "Shade", 0);

    fun getDisplayName(isGerman: Boolean = true): String {
        return if (isGerman) displayNameDE else displayNameEN
    }
}

/**
 * Companion plant relationship types
 */
enum class CompanionType(val displayNameDE: String, val displayNameEN: String, val colorHex: String) {
    GOOD("Guter Nachbar", "Good Companion", "#4CAF50"),      // Green
    NEUTRAL("Neutral", "Neutral", "#FFC107"),                 // Yellow
    BAD("Schlechter Nachbar", "Bad Companion", "#F44336");   // Red

    fun getDisplayName(isGerman: Boolean = true): String {
        return if (isGerman) displayNameDE else displayNameEN
    }
}

/**
 * Bed shape types
 */
enum class BedShape(val displayNameDE: String, val displayNameEN: String) {
    RECTANGLE("Rechteck", "Rectangle"),
    SQUARE("Quadrat", "Square"),
    L_SHAPE("L-Form", "L-Shape"),
    RAISED_BED("Hochbeet", "Raised Bed"),
    ROUND("Rund", "Round");

    fun getDisplayName(isGerman: Boolean = true): String {
        return if (isGerman) displayNameDE else displayNameEN
    }
}

/**
 * Task types for calendar
 */
enum class TaskType(val displayNameDE: String, val displayNameEN: String, val iconName: String) {
    SOW_INDOOR("Vorziehen", "Sow Indoor", "seedling"),
    SOW_OUTDOOR("Aussaat", "Sow Outdoor", "seed"),
    TRANSPLANT("Auspflanzen", "Transplant", "plant"),
    HARVEST("Ernte", "Harvest", "basket"),
    WATER("Gießen", "Water", "water_drop"),
    FERTILIZE("Düngen", "Fertilize", "nutrition"),
    PRUNE("Schneiden", "Prune", "scissors"),
    WEED("Unkraut jäten", "Weed", "grass"),
    PEST_CONTROL("Schädlingsbekämpfung", "Pest Control", "bug"),
    COMPOST("Kompost", "Compost", "compost"),
    OTHER("Sonstiges", "Other", "task");

    fun getDisplayName(isGerman: Boolean = true): String {
        return if (isGerman) displayNameDE else displayNameEN
    }
}

/**
 * Compost material types
 */
enum class CompostMaterialType(val displayNameDE: String, val displayNameEN: String, val isGreen: Boolean) {
    // Green materials (nitrogen-rich)
    KITCHEN_SCRAPS("Küchenabfälle", "Kitchen Scraps", true),
    GRASS_CLIPPINGS("Rasenschnitt", "Grass Clippings", true),
    FRESH_LEAVES("Frische Blätter", "Fresh Leaves", true),
    COFFEE_GROUNDS("Kaffeesatz", "Coffee Grounds", true),
    FRUIT_WASTE("Obstabfälle", "Fruit Waste", true),
    VEGETABLE_WASTE("Gemüseabfälle", "Vegetable Waste", true),

    // Brown materials (carbon-rich)
    DRY_LEAVES("Trockene Blätter", "Dry Leaves", false),
    STRAW("Stroh", "Straw", false),
    CARDBOARD("Karton", "Cardboard", false),
    WOOD_CHIPS("Holzschnitzel", "Wood Chips", false),
    SAWDUST("Sägespäne", "Sawdust", false),
    NEWSPAPER("Zeitungspapier", "Newspaper", false),
    EGG_CARTONS("Eierkartons", "Egg Cartons", false);

    fun getDisplayName(isGerman: Boolean = true): String {
        return if (isGerman) displayNameDE else displayNameEN
    }

    fun getCategory(): String {
        return if (isGreen) "Grün (Stickstoff)" else "Braun (Kohlenstoff)"
    }
}