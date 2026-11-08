package com.metrolist.music.raq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.metrolist.music.raq.ui.PlayerUi
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

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (onSeeAll != null) Text("See all >", color = Color.White.copy(.7f), fontSize = 12.sp, modifier = Modifier.clickable { onSeeAll() })
    }
}

@Composable
fun MiniPlayer(state: PlayerUi, onToggle: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val t = state.track ?: return
    Glass(modifier.fillMaxWidth().padding(horizontal = 12.dp), radius = 18.dp, onClick = onOpen) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(t.coverUrl, 40.dp, 10.dp)
            Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                Text(t.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.artist, color = Color.White.copy(.6f), fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onToggle) { Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White) }
            Icon(Icons.Outlined.Cast, null, tint = Color.White.copy(.8f), modifier = Modifier.padding(end = 6.dp))
        }
    }
}

enum class Tab(val label: String, val icon: ImageVector, val active: Color) {
    Home("Home", Icons.Filled.Home, Mint),
    Search("Search", Icons.Filled.Search, Mint),
    Library("Library", Icons.Filled.LibraryMusic, Accent),
    Profile("Profile", Icons.Filled.Person, Mint),
}

@Composable
fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.Black.copy(.35f)).navigationBarsPadding().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
        Tab.values().forEach { tab ->
            val on = tab == current
            Column(Modifier.clickable { onSelect(tab) }.padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(tab.icon, null, tint = if (on) tab.active else Color.White.copy(.55f))
                Text(tab.label, fontSize = 10.sp, color = if (on) tab.active else Color.White.copy(.55f))
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
