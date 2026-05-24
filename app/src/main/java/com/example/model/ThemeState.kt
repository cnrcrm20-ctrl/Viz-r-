package com.example.model

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VizorThemeColors(
    val primaryColorHex: String = "#2196F3",
    val canvasBgColorHex: String = "#FFFFFF",
    val textColorHex: String = "#1A1A1A",
    val incomingBubbleHex: String = "#F0F0F0",
    val outgoingBubbleHex: String = "#2196F3"
) {
    fun getPrimary() = Color(android.graphics.Color.parseColor(primaryColorHex))
    fun getCanvasBg() = Color(android.graphics.Color.parseColor(canvasBgColorHex))
    fun getTextColor() = Color(android.graphics.Color.parseColor(textColorHex))
    fun getIncoming() = Color(android.graphics.Color.parseColor(incomingBubbleHex))
    fun getOutgoing() = Color(android.graphics.Color.parseColor(outgoingBubbleHex))
}

object ThemeSettingsManager {
    private val _themeFlow = MutableStateFlow(VizorThemeColors())
    val themeFlow = _themeFlow.asStateFlow()

    fun updateTheme(newColors: VizorThemeColors, context: Context) {
        _themeFlow.value = newColors
        val prefs = context.getSharedPreferences("vizor_theme_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("primary", newColors.primaryColorHex)
            putString("canvas", newColors.canvasBgColorHex)
            putString("text", newColors.textColorHex)
            putString("incoming", newColors.incomingBubbleHex)
            putString("outgoing", newColors.outgoingBubbleHex)
            apply()
        }
    }

    fun loadTheme(context: Context) {
        val prefs = context.getSharedPreferences("vizor_theme_prefs", Context.MODE_PRIVATE)
        val defaultColors = VizorThemeColors()
        val loaded = VizorThemeColors(
            primaryColorHex = prefs.getString("primary", defaultColors.primaryColorHex) ?: defaultColors.primaryColorHex,
            canvasBgColorHex = prefs.getString("canvas", defaultColors.canvasBgColorHex) ?: defaultColors.canvasBgColorHex,
            textColorHex = prefs.getString("text", defaultColors.textColorHex) ?: defaultColors.textColorHex,
            incomingBubbleHex = prefs.getString("incoming", defaultColors.incomingBubbleHex) ?: defaultColors.incomingBubbleHex,
            outgoingBubbleHex = prefs.getString("outgoing", defaultColors.outgoingBubbleHex) ?: defaultColors.outgoingBubbleHex
        )
        _themeFlow.value = loaded
    }

    fun resetToDefault(context: Context) {
        updateTheme(VizorThemeColors(), context)
    }
}
