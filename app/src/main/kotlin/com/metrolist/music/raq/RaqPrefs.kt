package com.metrolist.music.raq

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Pengaturan milik RaqMusic sendiri (yang tidak ada di Metrolist). Disimpan di SharedPreferences "raq_prefs". */
object RaqPrefs {
    const val FILE = "raq_prefs"
    const val AUTO_LYRICS = "auto_lyrics"
    const val SYNCED_LYRICS = "synced_lyrics"
    const val STARTUP_TAB = "startup_tab" // "Home" | "Search" | "Library"

    fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    fun bool(ctx: Context, key: String, def: Boolean) = sp(ctx).getBoolean(key, def)
    fun string(ctx: Context, key: String, def: String) = sp(ctx).getString(key, def) ?: def
}

private class PrefState<T>(initial: T, private val save: (T) -> Unit) : MutableState<T> {
    private val st = mutableStateOf(initial)
    override var value: T
        get() = st.value
        set(v) { st.value = v; save(v) }
    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { value = it }
}

@Composable
fun rememberRaqBool(key: String, def: Boolean): MutableState<Boolean> {
    val ctx = LocalContext.current
    return remember(key) { PrefState(RaqPrefs.bool(ctx, key, def)) { RaqPrefs.sp(ctx).edit().putBoolean(key, it).apply() } }
}

@Composable
fun rememberRaqString(key: String, def: String): MutableState<String> {
    val ctx = LocalContext.current
    return remember(key) { PrefState(RaqPrefs.string(ctx, key, def)) { RaqPrefs.sp(ctx).edit().putString(key, it).apply() } }
}
