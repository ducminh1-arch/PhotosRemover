package com.photosremover.app.data.model

enum class SimilarSensitivity(val maxHammingDistance: Int, val label: String) {
    STRICT(3, "Strict (≤ 3 bits)"),
    NORMAL(5, "Normal (≤ 5 bits)"),
    LOOSE(10, "Loose (≤ 10 bits)")
}
