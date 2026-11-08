package com.metrolist.music.raq

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.raq.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Menyambungkan UI RaqMusic ke mesin Metrolist.
 * Dibuat di RaqAppHost (AppRoot.kt) dari LocalPlayerConnection & LocalDatabase.
 *
 * Perubahan dari versi sebelumnya:
 *  - Recently Played = riwayat putar asli (tabel event Metrolist + sesi ini), tidak lagi dicampur lagu yang di-like.
 *  - Mix (Chill Vibes, Top Hits, ...) disimpan sebagai cache di SharedPreferences: kalau pencarian YouTube
 *    gagal, cache terakhir tetap tampil; kalau belum ada cache, UI menampilkan tombol "coba lagi".
 *  - Playlist: buat / ganti nama / hapus / tambah lagu / buka detail.
 *  - Lagu dari database lokal (Liked, Playlist) diputar langsung dari MediaMetadata, tanpa cari ulang ke YouTube.
 */
class MetrolistBridge(
    private val pc: PlayerConnection,
    private val db: MusicDatabase,
    private val scope: CoroutineScope,
    context: Context,
) : MusicBridge {

    private val prefs = context.applicationContext.getSharedPreferences("raq_cache", Context.MODE_PRIVATE)

    private class MixDef(val id: String, val title: String, val subtitle: String, val query: String)

    private val mixDefs = listOf(
        MixDef("chill", "Chill Vibes", "Relax · Unwind · Breathe", "chill vibes songs"),
        MixDef("top", "Top Hits", "Lagu terpopuler", "top hits"),
        MixDef("daily", "Daily Mix", "Pilihan populer", "pop hits mix"),
        MixDef("chillhits", "Chill Hits", "Relax", "chill hits"),
        MixDef("focus", "Focus Flow", "Focus", "focus lofi beats"),
        MixDef("energy", "Energy Boost", "Workout", "workout gym energy songs"),
    )

    /** Hasil pencarian YouTube, supaya play() tidak perlu mencari ulang. */
    private val songCache = java.util.concurrent.ConcurrentHashMap<String, SongItem>()

    /** Lagu dari database lokal (Liked, Playlist, Riwayat) — bisa diputar langsung. */
    private val metaCache = java.util.concurrent.ConcurrentHashMap<String, MediaMetadata>()

    private val position = MutableStateFlow(0L)
    private val session = MutableStateFlow<List<Track>>(emptyList())
    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    private val _queueTitle = MutableStateFlow("Your Queue")
    private val _mixes = MutableStateFlow(mixDefs.map { Mix(it.id, it.title, it.subtitle, loadCachedMix(it.id)) })
    private val mixJobs = mutableMapOf<String, Job>()

    init {
        scope.launch {
            while (isActive) {
                runCatching { position.value = pc.player.currentPosition }
                delay(500)
            }
        }
        // Catat lagu yang benar-benar diputar di sesi ini
        scope.launch {
            pc.mediaMetadata.collect { m ->
                if (m != null) {
                    val t = Track(m.id, m.title, m.artists.joinToString { a -> a.name }, m.thumbnailUrl)
                    session.update { (listOf(t) + it).distinctBy { x -> x.id }.take(20) }
                }
            }
        }
        mixDefs.forEach { loadMix(it) }
    }

    // ---------------------------------------------------------------- Mix + cache

    private fun loadCachedMix(id: String): List<Track> = runCatching {
        val arr = JSONArray(prefs.getString("mix_$id", null) ?: return emptyList())
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Track(o.getString("id"), o.getString("title"), o.getString("artist"), o.optString("cover").ifBlank { null })
        }
    }.getOrDefault(emptyList())

    private fun saveCachedMix(id: String, tracks: List<Track>) {
        val arr = JSONArray()
        tracks.forEach {
            arr.put(JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist).put("cover", it.coverUrl ?: ""))
        }
        prefs.edit().putString("mix_$id", arr.toString()).apply()
    }

    private fun loadMix(d: MixDef) {
        mixJobs[d.id]?.cancel()
        mixJobs[d.id] = scope.launch(Dispatchers.IO) {
            _mixes.update { l -> l.map { if (it.id == d.id) it.copy(failed = false) else it } }
            val fresh = searchWithRetry(d.query).take(15)
            if (fresh.isNotEmpty()) saveCachedMix(d.id, fresh)
            _mixes.update { l ->
                l.map {
                    if (it.id != d.id) it
                    // gagal → pertahankan cache lama; failed hanya kalau benar-benar kosong
                    else if (fresh.isNotEmpty()) it.copy(tracks = fresh, failed = false)
                    else it.copy(failed = it.tracks.isEmpty())
                }
            }
        }
    }

    override fun retryMixes() {
        mixDefs.forEach { d -> if (_mixes.value.firstOrNull { it.id == d.id }?.let { it.tracks.isEmpty() } != false) loadMix(d) }
    }

    // ---------------------------------------------------------------- State

    override val player: StateFlow<PlayerUi> = combine(
        pc.mediaMetadata, pc.isPlaying, position, pc.shuffleModeEnabled, pc.repeatMode,
    ) { meta, playing, pos, shuffle, repeat ->
        PlayerUi(
            track = meta?.let { Track(it.id, it.title, it.artists.joinToString { a -> a.name }, it.thumbnailUrl) },
            isPlaying = playing,
            positionMs = pos,
            durationMs = ((meta?.duration ?: 1) * 1000L).coerceAtLeast(1),
            liked = meta?.liked ?: false,
            shuffle = shuffle,
            repeat = repeat != Player.REPEAT_MODE_OFF,
        )
    }.stateIn(scope, SharingStarted.Eagerly, PlayerUi())

    private fun MediaMetadata.cached(): Track {
        metaCache[id] = this
        return Track(id, title, artists.joinToString { a -> a.name }, thumbnailUrl)
    }

    override val library: StateFlow<List<PlaylistItem>> =
        db.playlistsByCreateDateAsc().map { list ->
            list.map { PlaylistItem(it.playlist.id, it.playlist.name, it.songCount, it.thumbnails.firstOrNull()) }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val madeForYou: StateFlow<List<PlaylistItem>> = library

    override val likedSongs: StateFlow<List<Track>> =
        db.likedSongsByCreateDateAsc().map { songs ->
            songs.reversed().map { it.toMediaMetadata().cached() }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Riwayat putar asli dari tabel event Metrolist (terbaru dulu). */
    private val history: StateFlow<List<Track>> =
        db.events().map { events ->
            events.map { it.song.toMediaMetadata().cached() }.distinctBy { it.id }.take(20)
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Sesi ini (langsung muncul) + riwayat tersimpan. Tidak ada lagi campuran lagu yang di-like. */
    override val recentlyPlayed: StateFlow<List<Track>> =
        combine(session, history) { now, saved -> (now + saved).distinctBy { it.id }.take(20) }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val mixes: StateFlow<List<Mix>> = _mixes

    override val trending: StateFlow<List<Track>> =
        _mixes.map { list -> list.firstOrNull { it.id == "top" }?.tracks.orEmpty() }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val queue: StateFlow<List<Track>> = _queue
    override val queueTitle: StateFlow<String> = _queueTitle

    // ---------------------------------------------------------------- Search / play

    private fun SongItem.toTrack() = Track(id, title, artists.joinToString { a -> a.name }, thumbnail)

    private suspend fun searchInternal(q: String): List<Track> {
        val items = YouTube.search(q, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            ?.items?.filterIsInstance<SongItem>() ?: return emptyList()
        items.forEach { songCache[it.id] = it }
        return items.map { it.toTrack() }
    }

    private suspend fun searchWithRetry(q: String): List<Track> {
        repeat(4) { attempt ->
            val r = runCatching { searchInternal(q) }.getOrDefault(emptyList())
            if (r.isNotEmpty()) return r
            delay(2000L * (attempt + 1))
        }
        return emptyList()
    }

    override suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        runCatching { searchInternal(query) }.getOrDefault(emptyList())
    }

    private suspend fun resolve(t: Track): SongItem? {
        songCache[t.id]?.let { return it }
        val found = runCatching {
            YouTube.search("${t.title} ${t.artist}", YouTube.SearchFilter.FILTER_SONG).getOrNull()
                ?.items?.filterIsInstance<SongItem>()
        }.getOrNull()
        found?.forEach { songCache[it.id] = it }
        return found?.firstOrNull { it.id == t.id } ?: found?.firstOrNull()
    }

    override fun play(track: Track) = playAll(listOf(track), 0, null)

    override fun playAll(tracks: List<Track>, startIndex: Int, title: String?) {
        if (tracks.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val i = startIndex.coerceIn(0, tracks.size - 1)
            // lagu yang dipilih diputar duluan, lalu sisanya mengikuti urutan daftar
            val ordered = tracks.drop(i) + tracks.take(i)
            val items = ArrayList<MediaItem>()
            val shown = ArrayList<Track>()
            ordered.forEachIndexed { k, t ->
                val local = metaCache[t.id]
                val cached = songCache[t.id]
                when {
                    local != null -> { items += local.toMediaItem(); shown += t }
                    cached != null -> { items += cached.toMediaItem(); shown += t }
                    k == 0 -> resolve(t)?.let { items += it.toMediaItem(); shown += it.toTrack() }
                }
            }
            if (items.isEmpty()) return@launch
            _queue.value = shown
            _queueTitle.value = title ?: "Your Queue"
            withContext(Dispatchers.Main) {
                pc.playQueue(ListQueue(title = title ?: shown.first().title, items = items))
            }
        }
    }

    // ---------------------------------------------------------------- Playlist
    // CATATAN: bagian ini memakai API database Metrolist. Kalau ada error compile, cukup sesuaikan
    // nama/parameter 5 pemanggilan db di bawah ini dengan versi Metrolist yang kamu pakai.

    override fun playlistTracks(id: String): Flow<List<Track>> =
        if (id == LIKED_ID) likedSongs
        else db.playlistSongs(id).map { list -> list.map { it.song.toMediaMetadata().cached() } }

    override fun createPlaylist(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        scope.launch(Dispatchers.IO) { db.query { insert(PlaylistEntity(name = n)) } }
    }

    override fun renamePlaylist(id: String, name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val entity = db.playlist(id).first()?.playlist ?: return@launch
            db.query { update(entity.copy(name = n)) }
        }
    }

    override fun deletePlaylist(id: String) {
        scope.launch(Dispatchers.IO) {
            val entity = db.playlist(id).first()?.playlist ?: return@launch
            db.query { delete(entity) }
        }
    }

    override fun addToPlaylist(playlistId: String, track: Track) {
        scope.launch(Dispatchers.IO) {
            val playlist = db.playlist(playlistId).first() ?: return@launch
            // pastikan lagu ada di database (lagu hasil pencarian YouTube belum tentu tersimpan)
            val meta = metaCache[track.id] ?: songCache[track.id]?.toMediaMetadata() ?: resolve(track)?.toMediaMetadata() ?: return@launch
            db.transaction {
                insert(meta)
                addSongsToPlaylist(playlist, listOf(meta.id))
            }
        }
    }

    // ---------------------------------------------------------------- Lirik

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

    private fun httpGet(url: String): String? {
        val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "RaqMusic/1.0")
        return try {
            if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            c.disconnect()
        }
    }

    private fun validLrc(v: String?) = v?.takeIf { it.isNotBlank() && it != "null" }

    /** Ambil satu jenis lirik dari LRCLIB: field = "plainLyrics" atau "syncedLyrics". */
    private fun fetchLrc(track: Track, field: String): String? {
        val title = enc(track.title)
        val artist = enc(track.artist.substringBefore(","))
        httpGet("https://lrclib.net/api/get?track_name=$title&artist_name=$artist")
            ?.let { validLrc(JSONObject(it).optString(field, "")) }
            ?.let { return it }
        val arr = httpGet("https://lrclib.net/api/search?q=${enc(track.title + " " + track.artist)}")
            ?.let { JSONArray(it) } ?: return null
        for (k in 0 until arr.length()) validLrc(arr.getJSONObject(k).optString(field, ""))?.let { return it }
        return null
    }

    /** Lirik biasa dari LRCLIB. null = tidak ditemukan atau jaringan gagal. */
    override suspend fun lyrics(track: Track): String? = withContext(Dispatchers.IO) {
        runCatching { fetchLrc(track, "plainLyrics") }.getOrNull()
    }

    private val lrcTag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    private fun parseLrc(text: String): List<LrcLine> =
        text.lineSequence().flatMap { line ->
            val tags = lrcTag.findAll(line).toList()
            val body = line.substring(tags.lastOrNull()?.range?.last?.plus(1) ?: 0).trim()
            tags.map { m ->
                val frac = m.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                LrcLine(m.groupValues[1].toLong() * 60_000 + m.groupValues[2].toLong() * 1000 + frac, body)
            }
        }.filter { it.text.isNotEmpty() }.sortedBy { it.timeMs }.toList()

    /** Lirik tersinkron (LRC) dari LRCLIB; null kalau lagu tidak punya timestamp. */
    override suspend fun syncedLyrics(track: Track): List<LrcLine>? = withContext(Dispatchers.IO) {
        runCatching { fetchLrc(track, "syncedLyrics")?.let(::parseLrc) }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    // ---------------------------------------------------------------- Pengaturan

    override fun audioSessionId(): Int = runCatching { pc.player.audioSessionId }.getOrDefault(0)

    private val _sleepEnd = MutableStateFlow<Long?>(null)
    override val sleepTimerEndsAt: StateFlow<Long?> = _sleepEnd
    private var sleepJob: Job? = null

    override fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null) { _sleepEnd.value = null; return }
        _sleepEnd.value = System.currentTimeMillis() + minutes * 60_000L
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            withContext(Dispatchers.Main) {
                val p = pc.player
                val original = p.volume
                try {
                    repeat(20) { p.volume = original * (1f - (it + 1) / 20f); delay(150) } // fade out ±3 detik
                    p.pause()
                } finally {
                    p.volume = original // kembalikan volume, termasuk kalau dibatalkan di tengah fade
                }
            }
            _sleepEnd.value = null
        }
    }

    override fun clearHistory() {
        session.value = emptyList()
        scope.launch(Dispatchers.IO) { db.query { clearListenHistory() } }
    }

    override fun togglePlay() = pc.togglePlayPause()
    override fun next() = pc.seekToNext()
    override fun previous() = pc.seekToPrevious()
    override fun seekTo(ms: Long) = pc.seekTo(ms)
    override fun toggleLike() = pc.toggleLike()
    override fun toggleShuffle() { pc.player.shuffleModeEnabled = !pc.player.shuffleModeEnabled }
    override fun toggleRepeat() {
        pc.player.repeatMode =
            if (pc.player.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }
}
