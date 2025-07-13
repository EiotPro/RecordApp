package com.example.recordapp.model

/**
 * Enum defining grid size options for PDF exports
 */
enum class GridSize(val columns: Int, val rows: Int, val displayName: String) {
    ONE_BY_ONE(1, 1, "Individual (1 per page)"),  // Individual images (one per page)
    TWO_BY_TWO(2, 2, "2×2 Grid (4 per page)"),    // 4 images per page
    TWO_BY_THREE(2, 3, "2×3 Grid (6 per page)"),   // 6 images per page
    TWO_BY_FOUR(2, 4, "2×4 Grid (8 per page)"),    // 8 images per page
    MAGAZINE_LAYOUT(3, 3, "Magazine Style Layout") // Special layout with varying cell sizes
} 