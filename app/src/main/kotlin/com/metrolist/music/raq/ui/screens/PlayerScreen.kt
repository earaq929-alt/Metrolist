package com.metrolist.music.raq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.raq.ui.*
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.theme.*

private fun fmt(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(bridge: MusicBridge, onClose: () -> Unit, onLyrics: () -> Unit) {
    val st by bridge.player.collectAsState()
    val t = st.track
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(30.dp).clickable { onClose() })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Playing from", color = Color.White.copy(.6f), fontSize = 11.sp)
                Text("Chill Vibes", color = Color.White, fontSize = 13.sp)
            }
            Icon(Icons.Filled.MoreHoriz, null, tint = Color.White)
        }
        Spacer(Modifier.height(16.dp))
        Cover(t?.coverUrl, 320.dp, 26.dp, Modifier.fillMaxWidth().aspectRatio(1f))
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t?.title ?: "-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(t?.artist ?: "", color = Color.White.copy(.65f), fontSize = 16.sp, maxLines = 1)
            }
            Icon(
                if (st.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null,
                tint = Accent, modifier = Modifier.size(28.dp).clickable { bridge.toggleLike() },
            )
        }
        Spacer(Modifier.height(14.dp))
        Slider(
            value = (st.positionMs.toFloat() / st.durationMs.coerceAtLeast(1)).coerceIn(0f, 1f),
            onValueChange = { bridge.seekTo((it * st.durationMs).toLong()) },
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(.25f)),
        )
        Row {
            Text(fmt(st.positionMs), color = Color.White.copy(.7f), fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(fmt(st.durationMs), color = Color.White.copy(.7f), fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Shuffle, null, tint = if (st.shuffle) Accent else Color.White, modifier = Modifier.clickable { bridge.toggleShuffle() })
            Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(38.dp).clickable { bridge.previous() })
            Box(Modifier.size(76.dp).clip(CircleShape).background(Color.White).clickable { bridge.togglePlay() }, contentAlignment = Alignment.Center) {
                Icon(if (st.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(38.dp))
            }
            Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(38.dp).clickable { bridge.next() })
            Icon(Icons.Filled.Repeat, null, tint = if (st.repeat) Accent else Color.White, modifier = Modifier.clickable { bridge.toggleRepeat() })
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Outlined.Cast, null, tint = Color.White.copy(.8f))
            Icon(Icons.Filled.QueueMusic, null, tint = Color.White.copy(.8f))
        }
        Spacer(Modifier.weight(1f))
        Glass(Modifier.fillMaxWidth().height(52.dp), radius = 26.dp, onClick = onLyrics) {
            Row(Modifier.padding(horizontal = 20.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text("Lyrics", color = Color.White, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = Color.White)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
