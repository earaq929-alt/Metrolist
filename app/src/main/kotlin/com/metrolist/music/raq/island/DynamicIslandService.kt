package com.metrolist.music.raq.island

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.metrolist.music.raq.ui.MusicBridge

/**
 * Dynamic Island: pil kecil di atas layar (di bawah status bar / poni kamera) yang menampilkan lagu yang
 * sedang diputar. Ketuk → membesar dengan kontrol prev / play / next; ketuk judul → buka aplikasi.
 *
 * Syarat: izin "Tampil di atas aplikasi lain" (SYSTEM_ALERT_WINDOW). Izin ini tidak bisa diminta lewat dialog
 * biasa, jadi pengguna dikirim ke layar pengaturan; AppRoot melanjutkan otomatis saat pengguna kembali.
 *
 * Service ini berjalan di proses yang sama dengan MusicService Metrolist, sehingga ikut hidup selama
 * musik diputar. `bridge` diisi oleh AppRoot sebelum service dijalankan.
 */
class DynamicIslandService : Service() {

    companion object {
        @Volatile var bridge: MusicBridge? = null

        const val PREFS = "raq_prefs"
        const val KEY_ENABLED = "island_enabled"

        fun isEnabled(ctx: Context) = ctx.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

        fun setEnabled(ctx: Context, on: Boolean) {
            ctx.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, on).apply()
            val i = Intent(ctx, DynamicIslandService::class.java)
            if (on) ctx.startService(i) else ctx.stopService(i)
        }
    }

    /** Pemilik lifecycle/state/viewmodel buatan sendiri; Compose butuh ini kalau tidak berada di dalam Activity. */
    private class Owner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
        override val viewModelStore = ViewModelStore()

        fun start() {
            controller.performAttach()
            controller.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            registry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
        }
    }

    private var windowManager: WindowManager? = null
    private var view: ComposeView? = null
    private val owner = Owner()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val b = bridge
        if (b == null || !Settings.canDrawOverlays(this)) {
            // tanpa izin / tanpa bridge (misalnya proses direstart) → jangan jalan
            stopSelf()
            return START_NOT_STICKY
        }
        if (view == null) show(b)
        return START_NOT_STICKY
    }

    private fun show(b: MusicBridge) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        owner.start()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (statusBarHeightPx() * 0.15f).toInt() // tepat di bawah tepi atas, di area status bar
        }

        val cv = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                IslandContent(b, onOpenApp = { openApp() })
            }
        }
        runCatching { wm.addView(cv, params) }.onFailure { stopSelf(); return }
        windowManager = wm
        view = cv
    }

    private fun statusBarHeightPx(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else (24 * resources.displayMetrics.density).toInt()
    }

    private fun openApp() {
        packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            ?.let { startActivity(it) }
    }

    override fun onDestroy() {
        view?.let { v -> runCatching { windowManager?.removeView(v) } }
        view = null
        owner.stop()
        super.onDestroy()
    }
}
