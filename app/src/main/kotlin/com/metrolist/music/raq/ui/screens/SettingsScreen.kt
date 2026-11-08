package com.metrolist.music.raq.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.constants.AudioNormalizationKey
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.constants.AutoLoadMoreKey
import com.metrolist.music.constants.CrossfadeEnabledKey
import com.metrolist.music.constants.CrossfadeGaplessKey
import com.metrolist.music.constants.PersistentQueueKey
import com.metrolist.music.constants.SkipSilenceKey
import com.metrolist.music.raq.RaqPrefs
import com.metrolist.music.raq.rememberRaqBool
import com.metrolist.music.raq.rememberRaqString
import com.metrolist.music.raq.ui.*
import com.metrolist.music.raq.ui.theme.*
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

/**
 * Pengaturan RaqMusic. Pengaturan Playback memakai kunci DataStore asli Metrolist, jadi langsung dibaca mesin
 * pemutar (dan tetap sinkron dengan layar pengaturan Metrolist). Pengaturan Lyrics / Startup disimpan di RaqPrefs.
 */
@Composable
fun SettingsScreen(
    bridge: MusicBridge,
    onBack: () -> Unit,
    onThemes: () -> Unit,
    onOpenMetrolist: () -> Unit,
    islandEnabled: Boolean,
    onIsland: (Boolean) -> Unit,
) {
    val ctx = LocalContext.current

    // --- Playback (kunci Metrolist) ---
    var crossfade by rememberPreference(CrossfadeEnabledKey, false)
    var gapless by rememberPreference(CrossfadeGaplessKey, true)
    var normalize by rememberPreference(AudioNormalizationKey, true)
    var skipSilence by rememberPreference(SkipSilenceKey, false)
    var autoplay by rememberPreference(AutoLoadMoreKey, true)
    var rememberQueue by rememberPreference(PersistentQueueKey, true)
    var quality by rememberEnumPreference(AudioQualityKey, AudioQuality.AUTO)

    // --- Lyrics & General (kunci RaqPrefs) ---
    var autoLyrics by rememberRaqBool(RaqPrefs.AUTO_LYRICS, true)
    var syncedLyrics by rememberRaqBool(RaqPrefs.SYNCED_LYRICS, true)
    var startup by rememberRaqString(RaqPrefs.STARTUP_TAB, "Home")

    val sleepEnd by bridge.sleepTimerEndsAt.collectAsState()
    var dialog by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Settings", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text("Customize your music experience ✨", color = Color.White.copy(.7f), fontSize = 15.sp)
            }
            CircleBtn(Icons.Filled.Close, onClick = onBack)
        }
        Spacer(Modifier.height(20.dp))

        // Kartu profil
        Glass(Modifier.fillMaxWidth().border(1.dp, Color.White.copy(.14f), RoundedCornerShape(28.dp)), radius = 28.dp) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Accent, Color(0xFF3B1E8F)))),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Person, null, tint = Color.White) }
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text("Raq", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Music Lover", color = Color.White.copy(.65f), fontSize = 14.sp)
                }
            }
        }

        Section("Appearance", Icons.Filled.Palette) {
            SettingRow("Theme", "Pilih tema & warna", Icons.Filled.Palette, onClick = onThemes)
        }

        Section("Playback", Icons.Filled.PlayArrow) {
            SettingRow("Equalizer", "Fine-tune your sound", Icons.Filled.Tune) { openEqualizer(ctx, bridge.audioSessionId()) }
            SettingRow("Crossfade", "Smooth transitions between songs", Icons.Filled.SwapHoriz, toggle = crossfade) { crossfade = it }
            SettingRow("Gapless Playback", "Play songs without gaps", Icons.Filled.GraphicEq, toggle = gapless) { gapless = it }
            SettingRow("Normalize Volume", "Keep volume consistent", Icons.Filled.VolumeUp, toggle = normalize) { normalize = it }
            SettingRow("Skip Silence", "Lewati bagian senyap", Icons.Filled.FastForward, toggle = skipSilence) { skipSilence = it }
            SettingRow("Autoplay", "Play next song automatically", Icons.Filled.PlaylistPlay, toggle = autoplay) { autoplay = it }
            SettingRow("Remember Queue", "Pulihkan antrean saat app dibuka", Icons.Filled.QueueMusic, toggle = rememberQueue) { rememberQueue = it }
            SettingRow(
                "Sleep Timer",
                sleepEnd?.let { "Berhenti pukul ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))}" } ?: "Off",
                Icons.Filled.Timer,
            ) { dialog = "sleep" }
            SettingRow("Audio Quality", qualityLabel(quality), Icons.Filled.HighQuality) { dialog = "quality" }
        }

        Section("Lyrics", Icons.Filled.MusicNote) {
            SettingRow("Auto Lyrics", "Automatically find lyrics", Icons.Filled.Subject, toggle = autoLyrics) { autoLyrics = it }
            SettingRow("Synced Lyrics", "Show lyrics in sync with the song", Icons.Filled.Sync, toggle = syncedLyrics) { syncedLyrics = it }
        }

        Section("General", Icons.Filled.Settings) {
            SettingRow("Startup Page", startup, Icons.Filled.Home) { dialog = "startup" }
            SettingRow("Dynamic Island", "Pil lagu di atas layar", Icons.Filled.PhoneAndroid, toggle = islandEnabled) { onIsland(it) }
            SettingRow("Clear History", "Hapus riwayat Recently Played", Icons.Filled.DeleteSweep) { dialog = "clear" }
            SettingRow("Backup & Restore", "Buka pengaturan Metrolist", Icons.Filled.Backup, onClick = onOpenMetrolist)
        }
        Spacer(Modifier.height(32.dp))
    }

    when (dialog) {
        "sleep" -> ChoiceDialog(
            "Sleep Timer",
            listOf("Off" to null, "15 menit" to 15, "30 menit" to 30, "45 menit" to 45, "1 jam" to 60, "2 jam" to 120),
            onDismiss = { dialog = null },
        ) {
            bridge.setSleepTimer(it)
            Toast.makeText(ctx, if (it == null) "Sleep timer dimatikan" else "Musik berhenti dalam $it menit", Toast.LENGTH_SHORT).show()
        }
        "quality" -> ChoiceDialog(
            "Audio Quality",
            AudioQuality.values().map { qualityLabel(it) to it },
            onDismiss = { dialog = null },
        ) { quality = it ?: AudioQuality.AUTO }
        "startup" -> ChoiceDialog(
            "Startup Page",
            listOf("Home", "Search", "Library").map { it to it },
            onDismiss = { dialog = null },
        ) { startup = it ?: "Home" }
        "clear" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Hapus riwayat?") },
            text = { Text("Daftar Recently Played akan dikosongkan. Lagu di library tidak ikut terhapus.") },
            confirmButton = { TextButton(onClick = { bridge.clearHistory(); dialog = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Batal") } },
        )
    }
}

private fun qualityLabel(q: AudioQuality) = when (q.name) {
    "AUTO" -> "Auto"
    "HIGH" -> "High"
    "LOW" -> "Low"
    else -> q.name.lowercase().replaceFirstChar { it.uppercase() }
}

/** Buka equalizer bawaan sistem untuk sesi audio player. */
private fun openEqualizer(ctx: android.content.Context, session: Int) {
    if (session == 0) {
        Toast.makeText(ctx, "Putar lagu dulu supaya equalizer bisa dipakai", Toast.LENGTH_SHORT).show()
        return
    }
    val i = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, session)
        .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, ctx.packageName)
        .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { ctx.startActivity(i) } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, "Perangkat ini tidak punya equalizer sistem", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun Section(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Row(Modifier.padding(top = 24.dp, bottom = 10.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Accent)
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 14.dp))
    }
    Glass(Modifier.fillMaxWidth().border(1.dp, Color.White.copy(.14f), RoundedCornerShape(28.dp)), radius = 28.dp) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}

/** Satu baris pengaturan: toggle != null → Switch; kalau tidak → panah ›. */
@Composable
private fun SettingRow(
    title: String, subtitle: String, icon: ImageVector,
    toggle: Boolean? = null, onToggle: (Boolean) -> Unit = {}, onClick: (() -> Unit)? = null,
) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth()
            .clickable { if (toggle != null) onToggle(!toggle) else onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(30.dp))
        Column(Modifier.padding(horizontal = 16.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.White.copy(.6f), fontSize = 13.sp)
        }
        if (toggle != null) {
            Switch(
                checked = toggle, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = Color.White),
            )
        } else {
            Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(.5f))
        }
    }
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<String, T?>>, onDismiss: () -> Unit, onPick: (T?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Text(
                        label,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(value); onDismiss() }.padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}
