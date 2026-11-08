package com.metrolist.music.raq.island

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.metrolist.music.raq.ui.MusicBridge
import com.metrolist.music.raq.ui.components.Cover

/**
 * Pill "Dynamic Island" di atas layar. Butuh izin "Display over other apps" (SYSTEM_ALERT_WINDOW).
 * Kecil (pill) saat ada lagu, tap -> expand: cover, judul, prev / play / next.
 *
 * Hubungkan dengan: DynamicIslandService.bridge = <implementasi MusicBridge Metrolist> sebelum start.
 */
class DynamicIslandService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object { var bridge: MusicBridge? = null }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var view: ComposeView? = null
    private lateinit var wm: WindowManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 14 }

        view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandService)
            setContent { Island() }
        }
        wm.addView(view, params)
    }

    @Composable
    private fun Island() {
        val b = bridge ?: return
        val st by b.player.collectAsState()
        val t = st.track ?: return
        var expanded by remember { mutableStateOf(false) }

        Box(
            Modifier.animateContentSize(spring(dampingRatio = .7f))
                .clip(RoundedCornerShape(if (expanded) 32.dp else 20.dp))
                .background(Color.Black)
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = if (expanded) 14.dp else 6.dp)
        ) {
            if (!expanded) {
                Row(Modifier.width(150.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(t.coverUrl, 22.dp, 11.dp)
                    Spacer(Modifier.weight(1f))
                    // visualizer mini
                    Text(if (st.isPlaying) "▂▅▃" else "▁▁▁", color = Color(0xFF2DE2C0), fontSize = 12.sp)
                }
            } else {
                Row(Modifier.width(320.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(t.coverUrl, 52.dp, 14.dp)
                    Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                        Text(t.title, color = Color.White, fontSize = 14.sp, maxLines = 1)
                        Text(t.artist, color = Color.White.copy(.6f), fontSize = 12.sp, maxLines = 1)
                    }
                    Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp).clickable { b.previous() })
                    Icon(
                        if (st.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp).size(34.dp).clickable { b.togglePlay() },
                    )
                    Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp).clickable { b.next() })
                }
            }
        }
    }

    override fun onDestroy() {
        view?.let { wm.removeView(it) }
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
}
