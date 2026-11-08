package com.metrolist.music.raq.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.raq.ui.*
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.theme.*

@Composable
fun SearchBar(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Glass(modifier.fillMaxWidth().padding(horizontal = 20.dp).height(48.dp), radius = 24.dp, onClick = onClick) {
        Row(Modifier.padding(horizontal = 14.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, tint = Color.White.copy(.8f))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text("Search songs, artists...", color = Color.White.copy(.5f), fontSize = 14.sp)
                BasicTextField(
                    value, onChange, enabled = onClick == null, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color.White),
                )
            }
            Icon(Icons.Filled.Mic, null, tint = Color.White.copy(.8f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(bridge: MusicBridge, onSearch: () -> Unit, onPlay: (Track) -> Unit) {
    val made by bridge.madeForYou.collectAsState()
    val recent by bridge.recentlyPlayed.collectAsState()
    val pager = rememberPagerState { 3 }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Good Morning,", color = Color.White, fontSize = 20.sp)
                    Text("Raq \uD83D\uDC4B", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Outlined.Notifications, null, tint = Color.White)
            }
            SearchBar("", {}, onClick = onSearch)
            Spacer(Modifier.height(16.dp))
            HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 20.dp), pageSpacing = 10.dp) { i ->
                val p = made.getOrNull(i)
                Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(22.dp))) {
                    Cover(p?.coverUrl, 400.dp, 0.dp, Modifier.fillMaxSize())
                    Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text(p?.name ?: "Chill Vibes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Relax · Unwind · Breathe", color = Color.White.copy(.8f), fontSize = 12.sp)
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(14.dp).size(44.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.Black) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.Center) {
                repeat(3) {
                    Box(
                        Modifier.padding(3.dp).size(6.dp).clip(CircleShape)
                            .background(if (pager.currentPage == it) Color.White else Color.White.copy(.35f))
                    )
                }
            }
            SectionHeader("Made for You") { }
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(made) { p ->
                    Box(Modifier.size(width = 112.dp, height = 140.dp).clip(RoundedCornerShape(18.dp))) {
                        Cover(p.coverUrl, 200.dp, 0.dp, Modifier.fillMaxSize())
                        Text(
                            p.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionHeader("Recently Played") { }
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recent) { t -> Cover(t.coverUrl, 96.dp, 16.dp, Modifier.clickable { onPlay(t) }) }
            }
        }
    }
}

@Composable
fun LibraryScreen(bridge: MusicBridge) {
    val lib by bridge.library.collectAsState()
    var tab by remember { mutableStateOf("Playlists") }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Your Library", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Search, null, tint = Color.White)
            Spacer(Modifier.width(16.dp))
            Icon(Icons.Filled.Add, null, tint = Color.White)
        }
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Playlists", "Artists", "Albums", "Songs").forEach { Chip(it, tab == it) { tab = it } }
        }
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(GlassFill), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, null, tint = Color.White)
                    }
                    Text("Create Playlist", color = Color.White, modifier = Modifier.padding(start = 14.dp), fontWeight = FontWeight.Medium)
                }
            }
            items(lib) { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Cover(p.coverUrl, 52.dp, 12.dp)
                    Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                        Text(p.name, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("${p.count} songs", color = Color.White.copy(.6f), fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(.5f))
                }
            }
        }
    }
}

@Composable
fun SearchScreen(bridge: MusicBridge, onPlay: (Track) -> Unit) {
    val trending by bridge.trending.collectAsState()
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Track>>(emptyList()) }
    LaunchedEffect(q) {
        if (q.length > 1) { kotlinx.coroutines.delay(350); results = bridge.search(q) } else results = emptyList()
    }
    val list = if (q.length > 1) results else trending
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Search", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))
        SearchBar(q, { q = it })
        Spacer(Modifier.height(16.dp))
        SectionHeader(if (q.length > 1) "Results" else "Trending Now")
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(list) { i, t ->
                Row(Modifier.clickable { onPlay(t) }, verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = Color.White, modifier = Modifier.width(28.dp))
                    Cover(t.coverUrl, 52.dp, 10.dp)
                    Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                        Text(t.title, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(t.artist, color = Color.White.copy(.6f), fontSize = 12.sp, maxLines = 1)
                    }
                    Icon(Icons.Filled.MoreHoriz, null, tint = Color.White.copy(.6f))
                }
            }
            if (q.length <= 1) item {
                Spacer(Modifier.height(8.dp))
                Text("Browse Categories", color = Color.White, fontWeight = FontWeight.SemiBold)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Pop" to Color(0xFFE8602C), "Hip-Hop" to Color(0xFFE0A81C), "Rock" to Color(0xFF7C3AED)).forEach { (n, c) ->
                        Box(
                            Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(14.dp)).background(c).padding(10.dp),
                            contentAlignment = Alignment.BottomStart,
                        ) { Text(n, color = Color.White, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}
