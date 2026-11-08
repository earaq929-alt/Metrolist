package com.metrolist.music.raq.ui.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.raq.RaqPrefs
import com.metrolist.music.raq.ui.*
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun fmt(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }

@Composable
fun PlayerScreen(bridge: MusicBridge, onClose: () -> Unit, onLyrics: () -> Unit = {}) {
    val st by bridge.player.collectAsState()
    val queue by bridge.queue.collectAsState()
    val queueTitle by bridge.queueTitle.collectAsState()
    val t = st.track
    val ctx = LocalContext.current

    // 0 = tidak ada panel, 1 = lirik, 2 = antrean
    var panel by remember { mutableStateOf(0) }
    var drag by remember { mutableStateOf<Float?>(null) }

    // Warna latar mengikuti cover lagu
    var coverColor by remember { mutableStateOf(Color(0xFF3B1E8F)) }
    LaunchedEffect(t?.coverUrl) {
        val url = t?.coverUrl ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(ctx).data(url).size(100, 100).allowHardware(false).build()
            val bmp = runCatching { ctx.imageLoader.execute(request).image?.toBitmap() }.getOrNull()
            if (bmp != null) {
                val p = Palette.from(bmp).maximumColorCount(8).generate()
                val rgb = p.vibrantSwatch?.rgb ?: p.dominantSwatch?.rgb
                if (rgb != null) coverColor = Color(rgb)
            }
        }
    }
    val bg by animateColorAsState(coverColor)

    // Lirik (dimuat saat panel lirik dibuka; pengaturan: Auto Lyrics & Synced Lyrics)
    val autoLyrics = remember { RaqPrefs.bool(ctx, RaqPrefs.AUTO_LYRICS, true) }
    val wantSynced = remember { RaqPrefs.bool(ctx, RaqPrefs.SYNCED_LYRICS, true) }
    var lyrics by remember(t?.id) { mutableStateOf<String?>(null) }
    var synced by remember(t?.id) { mutableStateOf<List<LrcLine>?>(null) }
    var lyricsLoading by remember(t?.id) { mutableStateOf(false) }
    var lyricsTried by remember(t?.id) { mutableStateOf(false) }
    var lyricsRequested by remember(t?.id) { mutableStateOf(false) } // dipakai kalau Auto Lyrics mati
    LaunchedEffect(panel, t?.id, lyricsRequested) {
        if (panel == 1 && t != null && !lyricsTried && (autoLyrics || lyricsRequested)) {
            lyricsLoading = true
            if (wantSynced) synced = bridge.syncedLyrics(t)
            if (synced == null) lyrics = bridge.lyrics(t)
            lyricsLoading = false
            lyricsTried = true
        }
    }

    BackHandler(enabled = panel != 0) { panel = 0 }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(bg, lerp(bg, Color.Black, 0.55f), lerp(bg, Color.Black, 0.88f)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleBtn(Icons.Filled.KeyboardArrowDown, onClick = onClose)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Playing from", color = Color.White.copy(.65f), fontSize = 12.sp)
                    Row(
                        Modifier.padding(top = 4.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(.16f))
                            .clickable { panel = 2 }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(queueTitle, color = Color.White, fontSize = 15.sp, maxLines = 1)
                        Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.padding(start = 4.dp).size(18.dp))
                    }
                }
                CircleBtn(Icons.Filled.MoreHoriz) { panel = 2 }
            }

            Spacer(Modifier.height(20.dp))
            Cover(
                t?.coverUrl, 320.dp, 36.dp,
                Modifier.fillMaxWidth().aspectRatio(1f).border(1.dp, Color.White.copy(.35f), RoundedCornerShape(36.dp)),
            )
            Spacer(Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t?.title ?: "-", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(t?.artist ?: "", color = Color.White.copy(.65f), fontSize = 20.sp, maxLines = 1)
                }
                CircleBtn(
                    if (st.liked) Icons.Filled.Star else Icons.Outlined.StarBorder, size = 52.dp,
                    tint = if (st.liked) Color(0xFFFFD54F) else Color.White,
                ) { bridge.toggleLike() }
                Spacer(Modifier.width(10.dp))
                CircleBtn(Icons.Filled.MoreHoriz, size = 52.dp) {
                    if (t != null) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${t.title} - ${t.artist}\nhttps://music.youtube.com/watch?v=${t.id}")
                        }
                        ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Slider(
                value = drag ?: (st.positionMs.toFloat() / st.durationMs.coerceAtLeast(1)).coerceIn(0f, 1f),
                onValueChange = { drag = it },
                onValueChangeFinished = {
                    drag?.let { bridge.seekTo((it * st.durationMs).toLong()) }
                    drag = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(.25f),
                ),
            )
            Row {
                Text(fmt(st.positionMs), color = Color.White.copy(.7f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(fmt(st.durationMs), color = Color.White.copy(.7f), fontSize = 13.sp)
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shuffle, null, tint = if (st.shuffle) Accent else Color.White, modifier = Modifier.size(28.dp).clickable { bridge.toggleShuffle() })
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(44.dp).clickable { bridge.previous() })
                Box(Modifier.size(84.dp).clip(CircleShape).background(Color.White).clickable { bridge.togglePlay() }, contentAlignment = Alignment.Center) {
                    Icon(if (st.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(42.dp))
                }
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(44.dp).clickable { bridge.next() })
                Icon(Icons.Filled.Repeat, null, tint = if (st.repeat) Accent else Color.White, modifier = Modifier.size(28.dp).clickable { bridge.toggleRepeat() })
            }

            Spacer(Modifier.height(24.dp))
            Glass(Modifier.fillMaxWidth().height(64.dp), radius = 32.dp) {
                Row(Modifier.padding(horizontal = 10.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    CircleBtn(Icons.Outlined.Cast, size = 44.dp) {
                        if (Build.VERSION.SDK_INT >= 29) {
                            runCatching {
                                ctx.startActivity(Intent(android.provider.Settings.Panel.ACTION_VOLUME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    CircleBtn(Icons.Filled.QueueMusic, size = 44.dp) { panel = 2 }
                }
            }
            Spacer(Modifier.height(12.dp))
            Glass(Modifier.fillMaxWidth().height(64.dp), radius = 32.dp, onClick = { panel = 1; onLyrics() }) {
                Row(Modifier.padding(horizontal = 10.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    CircleBtn(Icons.Filled.MusicNote, size = 44.dp)
                    Text("Lyrics", color = Color.White, fontSize = 17.sp, modifier = Modifier.padding(start = 14.dp).weight(1f))
                    Icon(Icons.Filled.ChevronRight, null, tint = Color.White, modifier = Modifier.padding(end = 10.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Panel Lirik / Antrean
        if (panel != 0) {
            Column(
                Modifier.fillMaxSize().background(Color.Black.copy(.92f)).statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 24.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (panel == 1) "Lyrics" else "Up Next", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (panel == 1) "${t?.title ?: ""} · ${t?.artist ?: ""}" else queueTitle,
                            color = Color.White.copy(.6f), fontSize = 13.sp, maxLines = 1,
                        )
                    }
                    CircleBtn(Icons.Filled.Close) { panel = 0 }
                }
                if (panel == 1) {
                    Box(Modifier.fillMaxSize()) {
                        val lines = synced
                        when {
                            !autoLyrics && !lyricsRequested && !lyricsTried -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Auto Lyrics sedang mati.", color = Color.White.copy(.7f))
                                Spacer(Modifier.height(12.dp))
                                Chip("Cari lirik", true) { lyricsRequested = true }
                            }
                            lyricsLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
                            lines != null -> {
                                val listState = rememberLazyListState()
                                val current = lines.indexOfLast { it.timeMs <= st.positionMs }.coerceAtLeast(0)
                                LaunchedEffect(current) { listState.animateScrollToItem((current - 2).coerceAtLeast(0)) }
                                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(vertical = 40.dp)) {
                                    itemsIndexed(lines) { i, l ->
                                        Text(
                                            l.text,
                                            color = Color.White.copy(if (i == current) 1f else 0.4f),
                                            fontSize = if (i == current) 26.sp else 22.sp,
                                            fontWeight = if (i == current) FontWeight.Bold else FontWeight.Medium,
                                            lineHeight = 32.sp,
                                            modifier = Modifier.fillMaxWidth().clickable { bridge.seekTo(l.timeMs) },
                                        )
                                    }
                                }
                            }
                            lyrics == null -> Text(
                                "Lirik untuk lagu ini tidak ditemukan.",
                                color = Color.White.copy(.7f), modifier = Modifier.align(Alignment.Center),
                            )
                            else -> Text(
                                lyrics ?: "", color = Color.White, fontSize = 20.sp, lineHeight = 32.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                            )
                        }
                    }
                } else {
                    if (queue.isEmpty()) {
                        Text("Antrean kosong. Putar lagu dari Home atau Search.", color = Color.White.copy(.7f))
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        itemsIndexed(queue) { i, q ->
                            TrackRow(q, playing = st.track?.id == q.id) { bridge.playAll(queue, i, queueTitle) }
                        }
                    }
                }
            }
        }
    }
}
