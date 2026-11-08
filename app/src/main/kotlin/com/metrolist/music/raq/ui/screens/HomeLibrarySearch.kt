package com.metrolist.music.raq.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.raq.ui.*
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.theme.*

@Composable
fun SearchBar(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Glass(
        modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp)
            .border(1.dp, Color.White.copy(.14f), RoundedCornerShape(26.dp)),
        radius = 26.dp, onClick = onClick,
    ) {
        Row(Modifier.padding(horizontal = 16.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, tint = Color.White.copy(.9f))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text("Search songs, artists...", color = Color.White.copy(.5f), fontSize = 15.sp)
                BasicTextField(
                    value, onChange, enabled = onClick == null, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(Color.White),
                )
            }
            Icon(Icons.Filled.GraphicEq, null, tint = AccentSoft)
        }
    }
}

private fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h < 11 -> "Good Morning,"
        h < 15 -> "Good Afternoon,"
        h < 19 -> "Good Evening,"
        else -> "Good Night,"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(bridge: MusicBridge, onSearch: () -> Unit, onPlay: (Track) -> Unit) {
    val ctx = LocalContext.current
    val mixes by bridge.mixes.collectAsState()
    val recent by bridge.recentlyPlayed.collectAsState()
    val trending by bridge.trending.collectAsState()
    val now by bridge.player.collectAsState()
    val pager = rememberPagerState { 3 }
    val heroColors = listOf(Color(0xFF6D28D9), Color(0xFF7E22CE), Color(0xFF4F46E5))
    var addTarget by remember { mutableStateOf<Track?>(null) }
    val topFailed = mixes.firstOrNull { it.id == "top" }?.failed == true

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.background(Brush.verticalGradient(listOf(Accent.copy(.30f), Color.Transparent))).statusBarsPadding()) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 20.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(greeting(), color = Color.White, fontSize = 22.sp)
                        Text("Raq \uD83D\uDC4B", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(.08f))
                            .border(1.dp, Color.White.copy(.18f), CircleShape)
                            .clickable { Toast.makeText(ctx, "Belum ada notifikasi", Toast.LENGTH_SHORT).show() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Notifications, null, tint = Color.White)
                        Box(Modifier.align(Alignment.TopEnd).padding(11.dp).size(8.dp).clip(CircleShape).background(Accent))
                    }
                }
                SearchBar("", {}, onClick = onSearch)
            }
            Spacer(Modifier.height(20.dp))

            HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 20.dp), pageSpacing = 12.dp) { i ->
                val m = mixes.getOrNull(i)
                val color = heroColors[i % heroColors.size]
                val cover = m?.tracks?.firstOrNull()?.coverUrl
                Box(
                    Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(color, Color(0xFF3B1E8F))))
                        .clickable {
                            if (m != null && m.tracks.isNotEmpty()) bridge.playAll(m.tracks, 0, m.title)
                            else if (m?.failed == true) bridge.retryMixes()
                        }
                ) {
                    if (cover != null) {
                        Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.62f).fillMaxHeight()) {
                            AsyncImage(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(color, Color.Transparent))))
                        }
                    }
                    Column(Modifier.align(Alignment.CenterStart).padding(start = 22.dp, bottom = 40.dp)) {
                        Text(m?.title ?: "", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                m == null -> ""
                                m.failed -> "Gagal memuat · ketuk untuk coba lagi"
                                m.tracks.isEmpty() -> "Memuat lagu..."
                                else -> m.subtitle
                            },
                            color = Color.White.copy(.85f), fontSize = 15.sp,
                        )
                    }
                    Box(
                        Modifier.align(Alignment.BottomStart).padding(22.dp).size(52.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(30.dp)) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                repeat(3) {
                    Box(
                        Modifier.padding(3.dp).size(if (pager.currentPage == it) 9.dp else 7.dp).clip(CircleShape)
                            .background(if (pager.currentPage == it) Color.White else Color.White.copy(.35f))
                    )
                }
            }

            SectionHeader("Made for You")
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(mixes.drop(2)) { m ->
                    Box(
                        Modifier.size(width = 124.dp, height = 150.dp).clip(RoundedCornerShape(20.dp))
                            .clickable { if (m.tracks.isNotEmpty()) bridge.playAll(m.tracks, 0, m.title) }
                    ) {
                        Cover(m.tracks.firstOrNull()?.coverUrl, 200.dp, 0.dp, Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.65f)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Text(m.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(m.subtitle, color = Color.White.copy(.7f), fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            SectionHeader("Recently Played")
            if (recent.isEmpty()) {
                Text(
                    "Putar satu lagu, nanti riwayatnya muncul di sini.",
                    color = Color.White.copy(.6f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
        }
        itemsIndexed(recent.take(4)) { i, t ->
            TrackRow(
                t, playing = now.isPlaying && now.track?.id == t.id,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                onMore = { addTarget = t },
            ) { bridge.playAll(recent, i, "Recently Played") }
        }
        item {
            Spacer(Modifier.height(14.dp))
            SectionHeader("Trending Now") { onSearch() }
            if (trending.isEmpty()) {
                Text(
                    if (topFailed) "Gagal memuat lagu. Ketuk untuk coba lagi." else "Memuat lagu...",
                    color = Color.White.copy(.6f), fontSize = 13.sp,
                    modifier = Modifier.clickable(enabled = topFailed) { bridge.retryMixes() }.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
        }
        itemsIndexed(trending.take(5)) { i, t ->
            TrackRow(
                t, playing = now.isPlaying && now.track?.id == t.id, index = i + 1,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                onMore = { addTarget = t },
            ) { bridge.playAll(trending, i, "Trending Now") }
        }
    }
    addTarget?.let { AddToPlaylistDialog(bridge, it) { addTarget = null } }
}

@Composable
fun LibraryScreen(bridge: MusicBridge, onOpenPlaylist: (PlaylistItem) -> Unit) {
    val lib by bridge.library.collectAsState()
    val liked by bridge.likedSongs.collectAsState()
    val now by bridge.player.collectAsState()
    var tab by remember { mutableStateOf("Playlists") }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var addTarget by remember { mutableStateOf<Track?>(null) }
    val likedItem = PlaylistItem(LIKED_ID, "Liked Songs", liked.size, null)

    val q = query.trim()
    val shownPlaylists = if (q.isEmpty()) lib else lib.filter { it.name.contains(q, ignoreCase = true) }
    val shownSongs = if (q.isEmpty()) liked else liked.filter { it.title.contains(q, true) || it.artist.contains(q, true) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Your Library", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(
                if (searching) Icons.Filled.Close else Icons.Filled.Search, null, tint = Color.White,
                modifier = Modifier.clickable { searching = !searching; if (!searching) query = "" },
            )
            Spacer(Modifier.width(18.dp))
            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.clickable { creating = true })
        }
        if (searching) {
            SearchBar(query, { query = it })
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Playlists", "Songs").forEach { Chip(it, tab == it) { tab = it } }
        }
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (tab == "Playlists") {
                item {
                    Row(Modifier.clickable { creating = true }, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(GlassFill), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, null, tint = Color.White)
                        }
                        Text("Create Playlist", color = Color.White, modifier = Modifier.padding(start = 14.dp), fontWeight = FontWeight.Medium)
                    }
                }
                if (q.isEmpty() || "liked songs".contains(q, ignoreCase = true)) item {
                    Row(Modifier.clickable { onOpenPlaylist(likedItem) }, verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Accent, Color(0xFF3B1E8F)))),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Favorite, null, tint = Color.White) }
                        Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                            Text("Liked Songs", color = Color.White, fontWeight = FontWeight.Medium)
                            Text("${liked.size} songs", color = Color.White.copy(.6f), fontSize = 12.sp)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(.5f))
                    }
                }
                items(shownPlaylists, key = { it.id }) { p ->
                    Row(Modifier.clickable { onOpenPlaylist(p) }, verticalAlignment = Alignment.CenterVertically) {
                        Cover(p.coverUrl, 56.dp, 14.dp)
                        Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                            Text(p.name, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text("${p.count} songs", color = Color.White.copy(.6f), fontSize = 12.sp)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(.5f))
                    }
                }
                if (q.isNotEmpty() && shownPlaylists.isEmpty()) item {
                    Text("Tidak ada playlist untuk \"$q\"", color = Color.White.copy(.6f), fontSize = 13.sp)
                }
            } else {
                itemsIndexed(shownSongs) { i, t ->
                    TrackRow(
                        t, playing = now.isPlaying && now.track?.id == t.id,
                        onMore = { addTarget = t },
                    ) { bridge.playAll(shownSongs, i, "Liked Songs") }
                }
                if (shownSongs.isEmpty()) item {
                    Text(if (q.isEmpty()) "Belum ada lagu yang kamu sukai." else "Tidak ada lagu untuk \"$q\"", color = Color.White.copy(.6f), fontSize = 13.sp)
                }
            }
        }
    }
    if (creating) NameDialog("Playlist baru", "Buat", onDismiss = { creating = false }) { bridge.createPlaylist(it) }
    addTarget?.let { AddToPlaylistDialog(bridge, it) { addTarget = null } }
}

private val categories = listOf(
    "Pop" to Color(0xFFE8602C), "Hip-Hop" to Color(0xFFE0A81C), "Rock" to Color(0xFF7C3AED),
    "R&B" to Color(0xFFE5486F), "EDM" to Color(0xFF1C7BE0), "Indie" to Color(0xFF1E8A4C),
)
private val popularSearches = listOf("The Weeknd", "Billie Eilish", "Taylor Swift", "Drake", "SZA", "Coldplay")

@Composable
fun SearchScreen(bridge: MusicBridge, onPlay: (Track) -> Unit) {
    val trending by bridge.trending.collectAsState()
    val now by bridge.player.collectAsState()
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Track>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var addTarget by remember { mutableStateOf<Track?>(null) }
    LaunchedEffect(q) {
        if (q.length > 1) {
            kotlinx.coroutines.delay(350)
            results = bridge.search(q)
            searched = true
        } else {
            results = emptyList()
            searched = false
        }
    }
    val searching = q.length > 1
    val list = if (searching) results else trending
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Search", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))
        SearchBar(q, { q = it })
        Spacer(Modifier.height(8.dp))
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!searching) {
                item {
                    Text("Browse Categories", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    categories.chunked(3).forEach { row ->
                        Row(Modifier.padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { (name, c) ->
                                Box(
                                    Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(listOf(c, c.copy(.55f))))
                                        .clickable { q = "$name music" }.padding(10.dp),
                                    contentAlignment = Alignment.BottomStart,
                                ) { Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                            }
                        }
                    }
                    Text("Popular Searches", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        popularSearches.forEach { Chip(it, false) { q = it } }
                    }
                    Text("Trending Now", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                    if (trending.isEmpty()) Text("Memuat lagu...", color = Color.White.copy(.6f), fontSize = 13.sp)
                }
            } else {
                item {
                    Text(
                        if (searched && results.isEmpty()) "Tidak ada hasil untuk \"$q\"" else "Results",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
            itemsIndexed(list) { i, t ->
                TrackRow(
                    t, playing = now.isPlaying && now.track?.id == t.id,
                    index = if (searching) null else i + 1,
                    onMore = { addTarget = t },
                ) { bridge.playAll(list, i, if (searching) "Search" else "Trending Now") }
            }
        }
    }
    addTarget?.let { AddToPlaylistDialog(bridge, it) { addTarget = null } }
}
