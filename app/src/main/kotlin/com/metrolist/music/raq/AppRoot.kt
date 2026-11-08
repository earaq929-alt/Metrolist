package com.metrolist.music.raq

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.widget.Toast
import com.metrolist.music.raq.ui.PlaylistItem
import com.metrolist.music.raq.island.DynamicIslandService
import com.metrolist.music.raq.ui.MusicBridge
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.screens.*
import com.metrolist.music.raq.ui.theme.*

/** true = tampilkan UI RaqMusic; false = UI asli Metrolist (restart app untuk kembali). */
object RaqSwitch { var useRaq by mutableStateOf(true) }

private enum class Overlay { None, Player, Themes, Settings, Playlist }

@Composable
fun AppRoot(bridge: MusicBridge) {
    val ctxForInit = LocalContext.current
    var palette by remember { mutableStateOf(AppPalette.Dynamic) }
    var tab by remember {
        mutableStateOf(
            Tab.values().firstOrNull { it.name == RaqPrefs.string(ctxForInit, RaqPrefs.STARTUP_TAB, "Home") } ?: Tab.Home
        )
    }
    var overlay by remember { mutableStateOf(Overlay.None) }
    var openPlaylist by remember { mutableStateOf<PlaylistItem?>(null) }
    val ctx = LocalContext.current
    val player by bridge.player.collectAsState()

    // Dynamic Island: status disimpan permanen, dan hanya dianggap aktif kalau izin overlay masih ada
    var island by remember { mutableStateOf(DynamicIslandService.isEnabled(ctx) && Settings.canDrawOverlays(ctx)) }
    var pendingIsland by remember { mutableStateOf(false) } // menunggu pengguna selesai memberi izin

    val applyIsland: (Boolean) -> Unit = { on ->
        island = on
        DynamicIslandService.bridge = bridge
        DynamicIslandService.setEnabled(ctx, on)
    }

    // Pulihkan island saat aplikasi dibuka lagi
    LaunchedEffect(Unit) {
        DynamicIslandService.bridge = bridge
        if (island) DynamicIslandService.setEnabled(ctx, true)
    }

    // Setelah pengguna kembali dari layar izin: lanjutkan mengaktifkan island otomatis
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME && pendingIsland) {
                pendingIsland = false
                if (Settings.canDrawOverlays(ctx)) applyIsland(true)
                else Toast.makeText(ctx, "Izin tampil di atas aplikasi lain belum diberikan", Toast.LENGTH_SHORT).show()
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    // Back: tutup overlay dulu, kalau di tab selain Home balik ke Home
    BackHandler(enabled = overlay != Overlay.None || tab != Tab.Home) {
        if (overlay != Overlay.None) overlay = Overlay.None else tab = Tab.Home
    }

    RaqTheme(palette) {
        when (overlay) {
            Overlay.Player -> PlayerScreen(bridge, { overlay = Overlay.None }, {})
            Overlay.Themes -> ThemesScreen(palette, { overlay = Overlay.None }) { palette = it; overlay = Overlay.None }
            Overlay.Settings -> SettingsScreen(
                bridge = bridge,
                onBack = { overlay = Overlay.None },
                onThemes = { overlay = Overlay.Themes },
                onOpenMetrolist = { RaqSwitch.useRaq = false },
                islandEnabled = island,
                onIsland = { on ->
                    if (on && !Settings.canDrawOverlays(ctx)) {
                        pendingIsland = true
                        ctx.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } else applyIsland(on)
                },
            )
            Overlay.Playlist -> openPlaylist?.let { pl ->
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) { PlaylistScreen(bridge, pl) { overlay = Overlay.None } }
                    MiniPlayer(player, bridge::togglePlay, { overlay = Overlay.Player }, onNext = bridge::next)
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
            Overlay.None -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.Home -> HomeScreen(bridge, { tab = Tab.Search }) { bridge.play(it) }
                        Tab.Search -> SearchScreen(bridge) { bridge.play(it) }
                        Tab.Library -> LibraryScreen(bridge) { openPlaylist = it; overlay = Overlay.Playlist }
                        Tab.Profile -> ProfileScreen({ overlay = Overlay.Settings }, { overlay = Overlay.Themes })
                    }
                }
                MiniPlayer(player, bridge::togglePlay, { overlay = Overlay.Player }, onNext = bridge::next)
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
    val appCtx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val bridge = remember(pc) { MetrolistBridge(pc, db, scope, appCtx) }
    AppRoot(bridge)
}
