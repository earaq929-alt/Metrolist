package com.metrolist.music.raq.island

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.raq.ui.MusicBridge
import kotlinx.coroutines.delay

private val IslandAccent = Color(0xFFA78BFA)

/**
 * Isi Dynamic Island.
 *  - Tersembunyi kalau belum ada lagu, atau sudah dijeda lebih dari 4 detik.
 *  - Mengecil (pil) → ketuk → membesar → otomatis mengecil lagi setelah 5 detik.
 * Sengaja tidak memakai tema aplikasi: warna dasar selalu hitam agar menyatu dengan poni kamera.
 */
@Composable
fun IslandContent(bridge: MusicBridge, onOpenApp: () -> Unit) {
    val st by bridge.player.collectAsState()
    val t = st.track

    var pausedLong by remember { mutableStateOf(false) }
    LaunchedEffect(st.isPlaying) {
        if (st.isPlaying) pausedLong = false else { delay(4000); pausedLong = true }
    }

    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(expanded, t?.id) {
        if (expanded) { delay(5000); expanded = false }
    }

    if (t == null || pausedLong) {
        // ukuran 0 → jendela overlay tidak menghalangi sentuhan ke aplikasi di bawahnya
        Spacer(Modifier.size(0.dp))
        return
    }

    val frac = (st.positionMs.toFloat() / st.durationMs.coerceAtLeast(1)).coerceIn(0f, 1f)

    Box(
        Modifier
            .animateContentSize(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
            .clip(RoundedCornerShape(if (expanded) 30.dp else 20.dp))
            .background(Color.Black)
            .clickable(enabled = !expanded) { expanded = true },
    ) {
        if (!expanded) {
            Row(
                Modifier.height(34.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(t.coverUrl, 24)
                Spacer(Modifier.width(18.dp))
                EqBars(st.isPlaying)
                Spacer(Modifier.width(6.dp))
            }
        } else {
            Column(Modifier.width(320.dp).padding(14.dp)) {
                Row(Modifier.fillMaxWidth().clickable { onOpenApp() }, verticalAlignment = Alignment.CenterVertically) {
                    Cover(t.coverUrl, 48)
                    Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                        Text(t.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t.artist, color = Color.White.copy(.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    EqBars(st.isPlaying)
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(.2f))) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(IslandAccent))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(34.dp).clip(CircleShape).clickable { bridge.previous(); expanded = true }.padding(4.dp))
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { bridge.togglePlay() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(if (st.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(26.dp)) }
                    Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(34.dp).clip(CircleShape).clickable { bridge.next(); expanded = true }.padding(4.dp))
                }
            }
        }
    }
}

@Composable
private fun Cover(url: String?, sizeDp: Int) {
    Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(IslandAccent.copy(.4f))) {
        if (url != null) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

/** Tiga batang equalizer kecil; bergerak hanya saat musik diputar. */
@Composable
private fun EqBars(playing: Boolean) {
    val tr = rememberInfiniteTransition(label = "eq")
    val heights = listOf(420, 560, 480).mapIndexed { i, ms ->
        tr.animateFloat(
            0.25f, 1f,
            infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Reverse, StartOffset(i * 90)),
            label = "bar$i",
        )
    }
    Row(Modifier.height(16.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        heights.forEach { h ->
            Box(Modifier.width(3.dp).fillMaxHeight(if (playing) h.value else 0.25f).clip(RoundedCornerShape(2.dp)).background(IslandAccent))
        }
    }
}
