package com.metrolist.music.raq.ui

import kotlinx.coroutines.flow.StateFlow

data class Track(val id: String, val title: String, val artist: String, val coverUrl: String?)
data class PlaylistItem(val id: String, val name: String, val count: Int, val coverUrl: String?)
data class PlayerUi(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 1,
    val liked: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
)

/**
 * JEMBATAN ke Metrolist. Implementasikan pakai kode Metrolist:
 *  player        -> PlayerConnection (ExoPlayer / MediaSession)
 *  search        -> YouTube.search(...) dari modul :innertube
 *  home/library  -> YouTube.home() / MusicDatabase (Room)
 *  lyrics        -> LyricsHelper (LrcLib / KuGou)
 */
interface MusicBridge {
    val player: StateFlow<PlayerUi>
    val madeForYou: StateFlow<List<PlaylistItem>>
    val recentlyPlayed: StateFlow<List<Track>>
    val library: StateFlow<List<PlaylistItem>>
    val trending: StateFlow<List<Track>>
    suspend fun search(query: String): List<Track>
    fun play(track: Track)
    fun togglePlay()
    fun next()
    fun previous()
    fun seekTo(ms: Long)
    fun toggleLike()
    fun toggleShuffle()
    fun toggleRepeat()
}
