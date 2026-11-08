package com.metrolist.music.raq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.theme.*

@Composable
fun ThemesScreen(selected: AppPalette, onBack: () -> Unit, onApply: (AppPalette) -> Unit) {
    var pick by remember { mutableStateOf(selected) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ChevronLeft, null, tint = Color.White, modifier = Modifier.align(Alignment.CenterStart).clickable { onBack() })
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Themes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Choose your favorite theme", color = Color.White.copy(.7f), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            GridCells.Fixed(3), Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(AppPalette.values().toList()) { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(.82f).clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(p.top, p.mid, p.bottom)))
                            .then(if (p == pick) Modifier.border(2.dp, Mint, RoundedCornerShape(16.dp)) else Modifier)
                            .clickable { pick = p },
                        contentAlignment = Alignment.BottomEnd,
                    ) { if (p == pick) Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.padding(8.dp)) }
                    Text(p.label, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF5B5BD6), AccentSoft))).clickable { onApply(pick) },
            contentAlignment = Alignment.Center,
        ) { Text("Apply Theme", color = Color.White, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun ProfileScreen(onSettings: () -> Unit, onThemes: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Cover(null, 96.dp, 48.dp)
        Text("RaqMusic", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Text("@raqmusic", color = Color.White.copy(.6f), fontSize = 13.sp)
        Glass(Modifier.padding(top = 10.dp), radius = 20.dp) {
            Text("\uD83D\uDC51 Premium", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("248" to "Playlists", "12.4K" to "Followers", "120" to "Following").forEach { (n, l) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(n, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(l, color = Color.White.copy(.6f), fontSize = 12.sp)
                }
            }
        }
        Glass(Modifier.fillMaxWidth(), radius = 20.dp, onClick = onThemes) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Accent))
                Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                    Text("Your Mood", color = Color.White, fontWeight = FontWeight.Medium)
                    Text("Perfect Playlist for your vibe", color = Color.White.copy(.6f), fontSize = 11.sp)
                }
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.Black)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        listOf(Icons.Outlined.Settings to "Settings", Icons.Outlined.HelpOutline to "Help & Support", Icons.Outlined.Info to "About RAQMUSIC")
            .forEachIndexed { i, (ic, t) ->
                Row(Modifier.fillMaxWidth().clickable { if (i == 0) onSettings() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(ic, null, tint = Color.White)
                    Text(t, color = Color.White, modifier = Modifier.padding(start = 14.dp).weight(1f))
                    Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(.5f))
                }
            }
        Spacer(Modifier.weight(1f))
        Glass(Modifier.fillMaxWidth().height(44.dp), radius = 22.dp) {
            Text("Log Out", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onThemes: () -> Unit, islandEnabled: Boolean, onIsland: (Boolean) -> Unit, onOpenMetrolist: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
        Text("Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() })
        Spacer(Modifier.height(16.dp))
        Glass(Modifier.fillMaxWidth(), radius = 20.dp) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(null, 52.dp, 26.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Raq Music", color = Color.White, fontWeight = FontWeight.Medium)
                    Text("@raqmusic", color = Color.White.copy(.6f), fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 20.dp) {
            Column {
                SettingRow(Icons.Outlined.Settings, "Appearance", "Dynamic", onThemes)
                SettingRow(Icons.Outlined.PlayCircle, "Playback") {}
                SettingRow(Icons.Outlined.Notifications, "Notifications") {}
                SettingRow(Icons.Outlined.Lock, "Privacy & Security") {}
                SettingRow(Icons.Outlined.Info, "About RAQMUSIC") {}
            }
        }
        Spacer(Modifier.height(14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 20.dp) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Dynamic Island", color = Color.White, modifier = Modifier.weight(1f))
                Switch(islandEnabled, onIsland)
            }
        }
        Spacer(Modifier.height(14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 20.dp, onClick = onOpenMetrolist) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Pengaturan lengkap Metrolist", color = Color.White, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(.5f))
            }
        }
        Spacer(Modifier.height(14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 20.dp) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("\u2B50", fontSize = 28.sp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Support Us", color = Color.White)
                    Text("Like our app? Give us a rating!", color = Color.White.copy(.6f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, trailing: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White)
        Text(title, color = Color.White, modifier = Modifier.padding(start = 14.dp).weight(1f))
        if (trailing != null) Text(trailing, color = Color.White.copy(.6f), fontSize = 11.sp, modifier = Modifier.padding(end = 6.dp))
        Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(.5f))
    }
}
