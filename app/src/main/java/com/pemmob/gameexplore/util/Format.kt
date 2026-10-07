package com.pemmob.gameexplore.util

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Rating dari RAWG berupa [Double] dengan skala 0.0–5.0.
 * Diputus 1 desimal dengan [Locale.ROOT] supaya selalu tampil "4.5",
 * bukan "4,5" yang mengikuti locale perangkat.
 */
fun formatRating(rating: Double?): String =
    rating?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "N/A"

/**
 * Representasi bintang dari rating, mis. 4.6 → ★★★★★ dan 3.4 → ★★★☆☆.
 * Nilai null (game tanpa rating) menghasilkan string kosong.
 */
fun ratingStars(rating: Double?): String {
    val filled = rating?.roundToInt()?.coerceIn(0, 5) ?: 0
    return "★".repeat(filled) + "☆".repeat(5 - filled)
}

