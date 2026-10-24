package com.example.data.openai

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class OpenAiAudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _activeFilePath = MutableStateFlow<String?>(null)
    val activeFilePath: StateFlow<String?> = _activeFilePath.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2.5f)
        _playbackSpeed.value = safeSpeed
        try {
            mediaPlayer?.let { player ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val wasPlaying = player.isPlaying
                    player.playbackParams = player.playbackParams.setSpeed(safeSpeed)
                    if (!wasPlaying && player.isPlaying) {
                        player.pause()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OpenAiAudioPlayer", "Error setting playback speed: ${e.message}")
        }
    }

    fun loadAndPlay(filePath: String, autoPlay: Boolean = true) {
        playFile(File(filePath), autoPlay = autoPlay, onCompletion = null)
    }

    fun playFile(file: File, autoPlay: Boolean = true, onCompletion: (() -> Unit)? = null) {
        val filePath = file.absolutePath
        if (!file.exists() || file.length() == 0L) {
            Log.e("OpenAiAudioPlayer", "Audio file not found or empty: $filePath")
            onCompletion?.invoke()
            return
        }

        try {
            stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(filePath)
                prepare()
                _durationMs.value = duration
                _activeFilePath.value = filePath

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && _playbackSpeed.value != 1.0f) {
                    try {
                        playbackParams = playbackParams.setSpeed(_playbackSpeed.value)
                    } catch (_: Exception) {}
                }

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                    stopProgressTracking()
                    onCompletion?.invoke()
                }
            }

            if (autoPlay) {
                play()
            }
        } catch (e: Exception) {
            Log.e("OpenAiAudioPlayer", "Error loading audio file: ${e.message}", e)
            _isPlaying.value = false
            onCompletion?.invoke()
        }
    }

    fun play() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && _playbackSpeed.value != 1.0f) {
                    try {
                        player.playbackParams = player.playbackParams.setSpeed(_playbackSpeed.value)
                    } catch (_: Exception) {}
                }
                player.start()
                _isPlaying.value = true
                startProgressTracking()
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
            _isPlaying.value = false
            stopProgressTracking()
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { player ->
            val safePos = positionMs.coerceIn(0, _durationMs.value)
            player.seekTo(safePos)
            _currentPositionMs.value = safePos
        }
    }

    fun skipForward(ms: Int = 5000) {
        seekTo(_currentPositionMs.value + ms)
    }

    fun skipBackward(ms: Int = 5000) {
        seekTo(_currentPositionMs.value - ms)
    }

    fun stop() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
            }
        } catch (_: Exception) {}
        _isPlaying.value = false
        _currentPositionMs.value = 0
        stopProgressTracking()
    }

    fun release() {
        stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _activeFilePath.value = null
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition
                        }
                    } catch (_: Exception) {}
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }
}

