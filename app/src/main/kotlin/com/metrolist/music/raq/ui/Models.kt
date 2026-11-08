package com.metrolist.music.raq.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class Track(val id: String, val title: String, val artist: String, val coverUrl: String?)
/** Satu baris lirik tersinkron (timeMs = kapan baris ini mulai dinyanyikan). */
data class LrcLine(val timeMs: Long, val text: String)
data class PlaylistItem(val id: String, val name: String, val count: Int, val coverUrl: String?)

/** Kumpulan lagu bertema (Chill Vibes, Top Hits, Daily Mix, ...). tracks kosong = masih dimuat. */
/** failed = pencarian sudah dicoba berkali-kali tapi tetap gagal dan belum ada cache. */
data class Mix(val id: String, val title: String, val subtitle: String, val tracks: List<Track>, val failed: Boolean = false)

/** id khusus untuk daftar "Liked Songs" (bukan playlist asli di database). */
const val LIKED_ID = "__liked__"

data class PlayerUi(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 1,
    val liked: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
)

/** Jembatan UI RaqMusic ke mesin Metrolist (implementasi: MetrolistBridge). */
interface MusicBridge {
    val player: StateFlow<PlayerUi>
    val madeForYou: StateFlow<List<PlaylistItem>>
    val recentlyPlayed: StateFlow<List<Track>>
    val library: StateFlow<List<PlaylistItem>>
    val likedSongs: StateFlow<List<Track>>
    val trending: StateFlow<List<Track>>
    val mixes: StateFlow<List<Mix>>
    val queue: StateFlow<List<Track>>
    val queueTitle: StateFlow<String>
    suspend fun search(query: String): List<Track>
    suspend fun lyrics(track: Track): String?
    fun play(track: Track)
    fun playAll(tracks: List<Track>, startIndex: Int = 0, title: String? = null)
    fun togglePlay()
    fun next()
    fun previous()
    fun seekTo(ms: Long)
    fun toggleLike()
    fun toggleShuffle()
    fun toggleRepeat()

    // ---- Playlist ----
    fun playlistTracks(id: String): Flow<List<Track>>
    fun createPlaylist(name: String)
    fun renamePlaylist(id: String, name: String)
    fun deletePlaylist(id: String)
    fun addToPlaylist(playlistId: String, track: Track)

    // ---- Pengaturan ----
    suspend fun syncedLyrics(track: Track): List<LrcLine>?
    /** Audio session ID player untuk Equalizer sistem; 0 kalau belum tersedia. */
    fun audioSessionId(): Int
    /** Waktu (epoch ms) sleep timer berakhir; null = tidak aktif. */
    val sleepTimerEndsAt: StateFlow<Long?>
    /** minutes == null → batalkan timer. Saat habis: volume diturunkan perlahan lalu dijeda. */
    fun setSleepTimer(minutes: Int?)
    /** Hapus riwayat putar (Recently Played) di database dan sesi ini. */
    fun clearHistory()

    /** Muat ulang Chill Vibes / Top Hits / dst. setelah gagal. */
    fun retryMixes()
}
