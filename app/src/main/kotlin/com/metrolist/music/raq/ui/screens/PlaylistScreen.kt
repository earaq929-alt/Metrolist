package com.metrolist.music.raq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.raq.ui.*
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.theme.*

/** Detail playlist: daftar lagu, Play, Shuffle, ganti nama, hapus. id == LIKED_ID untuk Liked Songs. */
@Composable
fun PlaylistScreen(bridge: MusicBridge, playlist: PlaylistItem, onBack: () -> Unit) {
    val isLiked = playlist.id == LIKED_ID
    val tracks by remember(playlist.id) { bridge.playlistTracks(playlist.id) }.collectAsState(initial = emptyList())
    val now by bridge.player.collectAsState()
    val lib by bridge.library.collectAsState()
    // nama terbaru (bisa berubah setelah rename)
    val name = if (isLiked) playlist.name else lib.firstOrNull { it.id == playlist.id }?.name ?: playlist.name

    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var addTarget by remember { mutableStateOf<Track?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleBtn(Icons.Filled.KeyboardArrowDown, onClick = onBack)
            Spacer(Modifier.weight(1f))
            if (!isLiked) {
                Box {
                    CircleBtn(Icons.Filled.MoreHoriz) { menu = true }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Ganti nama") }, onClick = { menu = false; renaming = true })
                        DropdownMenuItem(text = { Text("Hapus playlist") }, onClick = { menu = false; deleting = true })
                    }
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLiked) {
                        Box(
                            Modifier.size(104.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(listOf(Accent, Color(0xFF3B1E8F)))),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(44.dp)) }
                    } else {
                        Cover(playlist.coverUrl ?: tracks.firstOrNull()?.coverUrl, 104.dp, 22.dp)
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${tracks.size} lagu", color = Color.White.copy(.6f), fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(Color.White)
                            .then(if (tracks.isNotEmpty()) Modifier.clickable { bridge.playAll(tracks, 0, name) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(32.dp)) }
                    CircleBtn(Icons.Filled.Shuffle, size = 48.dp) {
                        if (tracks.isNotEmpty()) bridge.playAll(tracks.shuffled(), 0, name)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (tracks.isEmpty()) {
                    Text(
                        if (isLiked) "Belum ada lagu yang kamu sukai." else "Playlist masih kosong. Ketuk ⋯ pada lagu di Home atau Search, lalu pilih playlist ini.",
                        color = Color.White.copy(.6f), fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            itemsIndexed(tracks, key = { i, t -> "${t.id}_$i" }) { i, t ->
                TrackRow(
                    t, playing = now.isPlaying && now.track?.id == t.id,
                    onMore = { addTarget = t },
                ) { bridge.playAll(tracks, i, name) }
            }
        }
    }

    if (renaming) NameDialog("Ganti nama", "Simpan", initial = name, onDismiss = { renaming = false }) { bridge.renamePlaylist(playlist.id, it) }
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text("Hapus \"$name\"?") },
        text = { Text("Lagu di dalamnya tidak ikut terhapus dari library.") },
        confirmButton = { TextButton(onClick = { deleting = false; bridge.deletePlaylist(playlist.id); onBack() }) { Text("Hapus") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Batal") } },
    )
    addTarget?.let { AddToPlaylistDialog(bridge, it) { addTarget = null } }
}
