/**
 * RaqMusic palettes (dari layar "Themes" di desain).
 * seed dipakai sebagai themeColor MetrolistTheme; top/mid/bottom dipakai untuk background gradient (tahap berikutnya).
 */
package com.metrolist.music.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val RaqMint = Color(0xFF4EE3D0) // warna tab aktif di desain

enum class RaqPalette(
    val label: String,
    val seed: Color,
    val top: Color,
    val mid: Color,
    val bottom: Color,
) {
    Dynamic("Dynamic", Color(0xFFA855F7), Color(0xFF2A1B5C), Color(0xFF14102B), Color(0xFF05040C)),
    Aurora("Aurora", Color(0xFF2DE2C0), Color(0xFF0E7C6B), Color(0xFF0B3B4A), Color(0xFF050B12)),
    Sunset("Sunset", Color(0xFFE8602C), Color(0xFFE8602C), Color(0xFF5A1E5E), Color(0xFF0C0510)),
    Ocean("Ocean", Color(0xFF1C7BE0), Color(0xFF1C7BE0), Color(0xFF0B2A5E), Color(0xFF040A16)),
    Purple("Purple", Color(0xFF7C3AED), Color(0xFF7C3AED), Color(0xFF2B1260), Color(0xFF080414)),
    Rose("Rose", Color(0xFFE5486F), Color(0xFFE5486F), Color(0xFF55122E), Color(0xFF0D040A)),
    Forest("Forest", Color(0xFF1E8A4C), Color(0xFF1E8A4C), Color(0xFF0B3524), Color(0xFF040C08)),
    Gold("Gold", Color(0xFFE0A81C), Color(0xFFE0A81C), Color(0xFF5A3B0A), Color(0xFF0D0804)),
    Mono("Mono", Color(0xFF8A8A8A), Color(0xFF6B6B6B), Color(0xFF242424), Color(0xFF060606));

    val gradient: Brush get() = Brush.verticalGradient(listOf(top, mid, bottom))
}
