package com.metrolist.music.raq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.raq.ui.MusicBridge
import com.metrolist.music.raq.ui.PlayerUi
import com.metrolist.music.raq.ui.Track
import com.metrolist.music.raq.ui.theme.*

@Composable
fun Glass(modifier: Modifier = Modifier, radius: Dp = 20.dp, onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    val m = modifier.clip(RoundedCornerShape(radius)).background(GlassFill)
    Box(if (onClick != null) m.clickable { onClick() } else m, content = content)
}

@Composable
fun Cover(url: String?, size: Dp, radius: Dp = 14.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(RoundedCornerShape(radius)).background(Brush.linearGradient(listOf(Accent, Color(0xFF3B1E8F))))) {
        if (url != null) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

/** Tombol bulat transparan (chevron, titik tiga, bintang, dll). */
@Composable
fun CircleBtn(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 44.dp, tint: Color = Color.White, onClick: () -> Unit = {}) {
    Box(
        modifier.size(size).clip(CircleShape).background(Color.White.copy(.16f)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (onSeeAll != null) Text("See all →", color = Color.White.copy(.7f), fontSize = 13.sp, modifier = Modifier.clickable { onSeeAll() })
    }
}

/** Baris lagu: cover, judul, artis, ikon equalizer kalau sedang diputar. */
@Composable
fun TrackRow(track: Track, playing: Boolean, modifier: Modifier = Modifier, index: Int? = null, onMore: (() -> Unit)? = null, onClick: () -> Unit) {
    Glass(modifier.fillMaxWidth(), radius = 18.dp, onClick = onClick) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (index != null) Text("$index", color = Color.White.copy(.7f), fontSize = 14.sp, modifier = Modifier.width(26.dp).padding(start = 4.dp))
            Cover(track.coverUrl, 54.dp, 12.dp)
            Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                Text(track.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = Color.White.copy(.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (playing) Icon(Icons.Filled.GraphicEq, null, tint = Accent, modifier = Modifier.padding(end = 8.dp))
            Icon(
                Icons.Filled.MoreHoriz, null, tint = Color.White.copy(.6f),
                modifier = if (onMore != null) Modifier.clip(CircleShape).clickable { onMore() }.padding(6.dp) else Modifier,
            )
        }
    }
}

/** Mini player kapsul dengan garis progres ungu di bawah. */
@Composable
fun MiniPlayer(state: PlayerUi, onToggle: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier, onNext: () -> Unit = {}) {
    val t = state.track ?: return
    val frac = (state.positionMs.toFloat() / state.durationMs.coerceAtLeast(1)).coerceIn(0f, 1f)
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(shape).background(Color(0xE6150F2E))
            .border(1.dp, Accent.copy(.55f), shape)
            .clickable { onOpen() }
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(t.coverUrl, 50.dp, 12.dp)
            Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                Text(t.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.artist, color = Color.White.copy(.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onToggle) {
                Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
        Box(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 26.dp)
                .fillMaxWidth(frac).height(2.dp).background(Accent)
        )
    }
}

enum class Tab(val label: String, val icon: ImageVector, val active: Color) {
    Home("Home", Icons.Filled.Home, Accent),
    Search("Search", Icons.Filled.Search, Accent),
    Library("Library", Icons.Filled.LibraryMusic, Accent),
    Profile("Profile", Icons.Filled.Person, Accent),
}

@Composable
fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.Black.copy(.35f)).navigationBarsPadding().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
        Tab.values().forEach { tab ->
            val on = tab == current
            Column(Modifier.clickable { onSelect(tab) }.padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(tab.icon, null, tint = if (on) tab.active else Color.White.copy(.55f))
                Text(tab.label, fontSize = 11.sp, color = if (on) tab.active else Color.White.copy(.55f))
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if (selected) Accent else GlassFill).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Dialog satu kolom teks: dipakai untuk "Create Playlist" dan "Rename". */
@Composable
fun NameDialog(title: String, confirm: String, initial: String = "", onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(60) }, singleLine = true,
                placeholder = { Text("Nama playlist") },
            )
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

/** Pilih playlist tujuan untuk sebuah lagu; bisa juga langsung membuat playlist baru. */
@Composable
fun AddToPlaylistDialog(bridge: MusicBridge, track: Track, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val lib by bridge.library.collectAsState()
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        NameDialog("Playlist baru", "Buat", onDismiss = { creating = false }) { bridge.createPlaylist(it) }
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah ke playlist") },
        text = {
            LazyColumn {
                item {
                    Row(Modifier.fillMaxWidth().clickable { creating = true }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, null); Text("Playlist baru", modifier = Modifier.padding(start = 12.dp))
                    }
                }
                items(lib) { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            bridge.addToPlaylist(p.id, track)
                            Toast.makeText(ctx, "Ditambahkan ke ${p.name}", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Text(p.name, modifier = Modifier.weight(1f)); Text("${p.count}", color = Color.Gray) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}
