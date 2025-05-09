package com.example.recordapp.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Size specifications for dashboard widgets
 */
enum class WidgetSize {
    SMALL,  // 1x1 grid cell
    MEDIUM, // 1x2 grid cells
    LARGE,  // 2x2 grid cells
    FULL_WIDTH // Spans entire width, height of 1 cell
}

/**
 * Represents a position in the dashboard grid
 */
@Immutable
@Serializable
data class WidgetPosition(
    val row: Int,
    val column: Int
)

/**
 * Base dashboard widget model that represents different
 * types of widgets that can be placed on the dashboard
 */
@Serializable
sealed class DashboardWidget {
    abstract val id: String
    abstract val position: WidgetPosition
    abstract val size: WidgetSize
    abstract val title: String
    abstract val isVisible: Boolean
    
    // Common data for all widgets
    @Serializable
    data class Header(
        override val id: String = "header",
        override val position: WidgetPosition = WidgetPosition(0, 0),
        override val size: WidgetSize = WidgetSize.FULL_WIDTH,
        override val title: String = "Welcome",
        override val isVisible: Boolean = true
    ) : DashboardWidget()
    
    @Serializable
    data class FolderSelector(
        override val id: String = "folder_selector",
        override val position: WidgetPosition = WidgetPosition(1, 0),
        override val size: WidgetSize = WidgetSize.FULL_WIDTH,
        override val title: String = "Folders",
        override val isVisible: Boolean = true
    ) : DashboardWidget()
    
    @Serializable
    data class FinancialSummary(
        override val id: String = "financial_summary",
        override val position: WidgetPosition = WidgetPosition(2, 0),
        override val size: WidgetSize = WidgetSize.FULL_WIDTH,
        override val title: String = "Summary",
        override val isVisible: Boolean = true
    ) : DashboardWidget()
    
    @Serializable
    data class RecentTransactions(
        override val id: String = "recent_transactions",
        override val position: WidgetPosition = WidgetPosition(3, 0),
        override val size: WidgetSize = WidgetSize.FULL_WIDTH,
        override val title: String = "Recent Transactions",
        override val isVisible: Boolean = true,
        val showCount: Int = 3
    ) : DashboardWidget()
    
    // Function to create a default dashboard layout
    companion object {
        fun defaultWidgets(): List<DashboardWidget> = listOf(
            Header(),
            FolderSelector(),
            FinancialSummary(),
            RecentTransactions()
        )
    }
} 