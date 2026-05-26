package com.videoplyrio.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.view.TextureView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    data class PlaylistEntry(val title: String, val src: String)

    private val _playlist = MutableStateFlow<List<PlaylistEntry>>(emptyList())
    val playlist: StateFlow<List<PlaylistEntry>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    var controlsVisible by mutableStateOf(true)
        private set

    var playlistOpen by mutableStateOf(true)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var currentTitle by mutableStateOf("")
        private set

    var currentPosition by mutableStateOf(0L)
        private set

    var duration by mutableStateOf(0L)
        private set

    var bufferedPosition by mutableStateOf(0L)
        private set

    var volume by mutableStateOf(0.5f)
        private set

    var isMuted by mutableStateOf(false)
        private set

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private val hideControlsRunnable = Runnable { controlsVisible = false }
    private val handler = Handler(Looper.getMainLooper())

    private var positionUpdateJob: kotlinx.coroutines.Job? = null

    private val extractorEngine = ExtractorEngine(
        context = application,
        onStreamFound = { url -> viewModelScope.launch { playDirectUrl(url) } },
        onError = { msg -> errorMessage = msg; isLoading = false }
    )

    fun loadFromBase64(base64Data: String) {
        try {
            val json = String(android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT))
            val items = com.google.gson.Gson().fromJson(json, Array<PlaylistEntry>::class.java)
            _playlist.value = items.toList()
            val first = items.firstOrNull() ?: return
            currentTitle = first.title
            loadStream(first.src)
        } catch (e: Exception) {
            errorMessage = "فشل تحليل الرابط"
        }
    }

    fun loadFromIntent(url: String, title: String) {
        currentTitle = title
        _playlist.value = listOf(PlaylistEntry(title, url))
        loadStream(url)
    }

    private fun loadStream(url: String) {
        isLoading = true
        errorMessage = null

        val route = StreamRouter.route(url)
        when (route) {
            is StreamRouter.RouteResult.DirectPlay -> playDirectUrl(StreamRouter.stripSuffix(url))
            is StreamRouter.RouteResult.NeedsExtraction -> {
                if (route.method == StreamRouter.ExtractionMethod.NATIVE_PACKER) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val cleanUrl = StreamRouter.stripSuffix(url)
                        val result = NativeUnpacker.tryExtractFromUrl(cleanUrl, DESKTOP_UA)
                        if (result != null) playDirectUrl(result)
                        else extractorEngine.extract(cleanUrl)
                    }
                } else {
                    extractorEngine.extract(StreamRouter.stripSuffix(url))
                }
            }
        }
    }

    private fun playDirectUrl(url: String) {
        isLoading = false
        val mediaItem = MediaItem.fromUri(url)
        player.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        startPositionUpdates()
        resetControlsTimer()
    }

    fun playAt(index: Int) {
        _currentIndex.value = index
        val entry = _playlist.value.getOrNull(index) ?: return
        currentTitle = entry.title
        loadStream(entry.src)
    }

    fun retry() {
        val current = _playlist.value.getOrNull(_currentIndex.value) ?: return
        loadStream(current.src)
    }

    fun toggleControls() {
        controlsVisible = !controlsVisible
        if (controlsVisible) resetControlsTimer()
    }

    fun showControls() {
        controlsVisible = true
        resetControlsTimer()
    }

    private fun resetControlsTimer() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 3000L)
    }

    fun togglePlaylist() { playlistOpen = !playlistOpen }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        player.volume = if (isMuted) 0f else volume
    }

    fun toggleMute() {
        isMuted = !isMuted
        player.volume = if (isMuted) 0f else volume
    }

    fun attachTextureView(textureView: TextureView) {
        player.setVideoTextureView(textureView)
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                currentPosition = player.currentPosition
                duration = player.duration
                bufferedPosition = player.bufferedPosition
                kotlinx.coroutines.delay(250)
            }
        }
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun getAvailableSpeeds(): List<Float> = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

    fun getCurrentSpeed(): Float = player.playbackParameters.speed

    override fun onCleared() {
        extractorEngine.stop()
        positionUpdateJob?.cancel()
        player.release()
        super.onCleared()
    }

    companion object {
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
