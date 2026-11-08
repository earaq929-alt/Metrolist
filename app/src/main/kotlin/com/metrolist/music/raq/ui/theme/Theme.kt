package com.metrolist.music.raq.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFA855F7)
val AccentSoft = Color(0xFFC4B5FD)
val Mint = Color(0xFF2DE2C0)
val GlassFill = Color(0x1AFFFFFF)

enum class AppPalette(val label: String, val top: Color, val mid: Color, val bottom: Color) {
    Dynamic("Dynamic", Color(0xFF2A1B5C), Color(0xFF14102B), Color(0xFF05040C)),
    Aurora("Aurora", Color(0xFF0E7C6B), Color(0xFF0B3B4A), Color(0xFF050B12)),
    Sunset("Sunset", Color(0xFFE8602C), Color(0xFF5A1E5E), Color(0xFF0C0510)),
    Ocean("Ocean", Color(0xFF1C7BE0), Color(0xFF0B2A5E), Color(0xFF040A16)),
    Purple("Purple", Color(0xFF7C3AED), Color(0xFF2B1260), Color(0xFF080414)),
    Rose("Rose", Color(0xFFE5486F), Color(0xFF55122E), Color(0xFF0D040A)),
    Forest("Forest", Color(0xFF1E8A4C), Color(0xFF0B3524), Color(0xFF040C08)),
    Gold("Gold", Color(0xFFE0A81C), Color(0xFF5A3B0A), Color(0xFF0D0804)),
    Mono("Mono", Color(0xFF6B6B6B), Color(0xFF242424), Color(0xFF060606));
}

@Composable
fun RaqTheme(palette: AppPalette, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = palette.bottom)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(palette.top, palette.mid, palette.bottom)))) { content() }
    }
}
