package com.metrolist.music.raq

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.metrolist.music.raq.island.DynamicIslandService
import com.metrolist.music.raq.ui.MusicBridge
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.screens.*
import com.metrolist.music.raq.ui.theme.*

/** true = tampilkan UI RaqMusic; false = UI asli Metrolist (restart app untuk kembali). */
object RaqSwitch { var useRaq by mutableStateOf(true) }

private enum class Overlay { None, Player, Themes, Settings }

@Composable
fun AppRoot(bridge: MusicBridge) {
    var palette by remember { mutableStateOf(AppPalette.Dynamic) }
    var tab by remember { mutableStateOf(Tab.Home) }
    var overlay by remember { mutableStateOf(Overlay.None) }
    var island by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val player by bridge.player.collectAsState()

    // Back: tutup overlay dulu, kalau di tab selain Home balik ke Home
    BackHandler(enabled = overlay != Overlay.None || tab != Tab.Home) {
        if (overlay != Overlay.None) overlay = Overlay.None else tab = Tab.Home
    }

    RaqTheme(palette) {
        when (overlay) {
            Overlay.Player -> PlayerScreen(bridge, { overlay = Overlay.None }, {})
            Overlay.Themes -> ThemesScreen(palette, { overlay = Overlay.None }) { palette = it; overlay = Overlay.None }
            Overlay.Settings -> SettingsScreen(
                onBack = { overlay = Overlay.None },
                onThemes = { overlay = Overlay.Themes },
                onOpenMetrolist = { RaqSwitch.useRaq = false },
                islandEnabled = island,
                onIsland = { on ->
                    if (on && !Settings.canDrawOverlays(ctx)) {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } else {
                        island = on
                        DynamicIslandService.bridge = bridge
                        val i = Intent(ctx, DynamicIslandService::class.java)
                        if (on) ctx.startService(i) else ctx.stopService(i)
                    }
                },
            )
            Overlay.None -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.Home -> HomeScreen(bridge, { tab = Tab.Search }) { bridge.play(it) }
                        Tab.Search -> SearchScreen(bridge) { bridge.play(it) }
                        Tab.Library -> LibraryScreen(bridge)
                        Tab.Profile -> ProfileScreen({ overlay = Overlay.Settings }, { overlay = Overlay.Themes })
                    }
                }
                MiniPlayer(player, bridge::togglePlay, { overlay = Overlay.Player })
                BottomNav(tab) { tab = it }
            }
        }
    }
}

/** Titik masuk dari MainActivity: ambil PlayerConnection & database Metrolist, lalu tampilkan UI RaqMusic. */
@Composable
fun RaqAppHost() {
    val pc = com.metrolist.music.LocalPlayerConnection.current
    val db = com.metrolist.music.LocalDatabase.current
    val scope = rememberCoroutineScope()
    if (pc == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }
    val bridge = remember(pc) { MetrolistBridge(pc, db, scope) }
    AppRoot(bridge)
}
