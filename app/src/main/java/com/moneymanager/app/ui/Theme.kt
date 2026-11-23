package com.moneymanager.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val IncomeBlue = Color(0xFF2563EB)
val ExpenseRed = Color(0xFFE84C3D)
val TransferSlate = Color(0xFF64748B)
val BudgetGreen = Color(0xFF10B981)
val BudgetAmber = Color(0xFFF59E0B)

val AccentSwatches = listOf(
    0xFFE84C3D to "Coral",
    0xFF10B981 to "Emerald",
    0xFF2563EB to "Ocean",
    0xFF7C3AED to "Violet",
    0xFFD97706 to "Amber",
    0xFF334155 to "Slate"
)

@Composable
fun MoneyManagerTheme(
    darkMode: Boolean,
    accentHex: Long,
    content: @Composable () -> Unit
) {
    val accent = Color(accentHex)
    val scheme = if (darkMode) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            surfaceVariant = Color(0xFF263348),
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC)
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = Color(0xFFF3F4F6),
            surface = Color.White,
            surfaceVariant = Color(0xFFF8FAFC),
            onBackground = Color(0xFF111827),
            onSurface = Color(0xFF111827)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
