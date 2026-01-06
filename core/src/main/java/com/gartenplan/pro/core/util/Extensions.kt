package com.gartenplan.pro.core.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Extension functions used throughout the app
 */

// Flow Extensions
fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this
        .map<T, Result<T>> { Result.Success(it) }
        .onStart { emit(Result.Loading) }
        .catch { emit(Result.Error(it, it.message)) }
}

// String Extensions
fun String.capitalizeFirst(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
}

fun String?.orEmpty(default: String = ""): String {
    return this ?: default
}

// Date Extensions
fun LocalDate.toGermanMonth(): String {
    return this.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)
}

fun Month.toGermanName(): String {
    return this.getDisplayName(TextStyle.FULL, Locale.GERMAN)
}

fun Month.toShortGermanName(): String {
    return this.getDisplayName(TextStyle.SHORT, Locale.GERMAN)
}

fun Int.toMonth(): Month {
    return Month.of(this.coerceIn(1, 12))
}

// List Extensions
fun <T> List<T>.secondOrNull(): T? = this.getOrNull(1)

fun <T> List<T>.thirdOrNull(): T? = this.getOrNull(2)

// Number Extensions
fun Int.cmToMeter(): Float = this / 100f

fun Float.meterToCm(): Int = (this * 100).toInt()

fun Int.toSquareMeter(widthCm: Int): Float {
    return (this * widthCm) / 10000f
}

// Nullable Extensions
inline fun <T, R> T?.ifNotNull(block: (T) -> R): R? {
    return if (this != null) block(this) else null
}

inline fun <T> T?.ifNull(block: () -> T): T {
    return this ?: block()
}