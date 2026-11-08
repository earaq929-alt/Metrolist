package com.metrolist.music.raq

import androidx.media3.common.Player
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.raq.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Menyambungkan UI RaqMusic ke mesin Metrolist.
 * Dipakai di MainActivity (di dalam CompositionLocalProvider yang sudah ada):
 *   val pc = LocalPlayerConnection.current ; val db = LocalDatabase.current
 *   val bridge = remember(pc) { MetrolistBridge(pc!!, db, scope) }
 *   AppRoot(bridge)
 */
class MetrolistBridge(
    private val pc: PlayerConnection,
    private val db: MusicDatabase,
    private val scope: CoroutineScope,
) : MusicBridge {

    private val position = MutableStateFlow(0L)

    init {
        scope.launch {
            while (isActive) {
                runCatching { position.value = pc.player.currentPosition }
                delay(500)
            }
        }
    }

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

    override val library: StateFlow<List<PlaylistItem>> =
        db.playlistsByCreateDateAsc().map { list ->
            list.map { PlaylistItem(it.playlist.id, it.playlist.name, it.songCount, it.thumbnails.firstOrNull()) }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val madeForYou: StateFlow<List<PlaylistItem>> = library

    override val recentlyPlayed: StateFlow<List<Track>> =
        db.likedSongsByCreateDateAsc().map { songs ->
            songs.takeLast(20).reversed().map {
                val m = it.toMediaMetadata()
                Track(m.id, m.title, m.artists.joinToString { a -> a.name }, m.thumbnailUrl)
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _trending = MutableStateFlow<List<Track>>(emptyList())
    override val trending: StateFlow<List<Track>> = _trending

    init {
        scope.launch(Dispatchers.IO) {
            _trending.value = runCatching { searchInternal("top hits") }.getOrDefault(emptyList()).take(5)
        }
    }

    private suspend fun searchInternal(q: String): List<Track> =
        YouTube.search(q, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items
            ?.filterIsInstance<SongItem>()
            ?.map { Track(it.id, it.title, it.artists.joinToString { a -> a.name }, it.thumbnail) }
            ?: emptyList()

    override suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) { searchInternal(query) }

    override fun play(track: Track) {
        scope.launch(Dispatchers.IO) {
            val song = YouTube.search(track.title + " " + track.artist, YouTube.SearchFilter.FILTER_SONG)
                .getOrNull()?.items?.filterIsInstance<SongItem>()?.firstOrNull { it.id == track.id }
                ?: YouTube.search(track.title, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items
                    ?.filterIsInstance<SongItem>()?.firstOrNull()
                ?: return@launch
            withContext(Dispatchers.Main) {
                pc.playQueue(ListQueue(title = track.title, items = listOf(song.toMediaItem())))
            }
        }
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
