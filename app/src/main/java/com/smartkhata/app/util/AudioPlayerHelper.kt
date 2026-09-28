package com.smartkhata.app.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object AudioPlayerHelper {
    private var mediaPlayer: MediaPlayer? = null
    private val _currentlyPlayingPath = MutableStateFlow<String?>(null)
    val currentlyPlayingPath: StateFlow<String?> = _currentlyPlayingPath.asStateFlow()

    fun play(filePath: String, onCompletion: () -> Unit = {}) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioPlayerHelper", "File does not exist: $filePath")
            return
        }

        // If same file is already playing, stop/pause it
        if (_currentlyPlayingPath.value == filePath) {
            stop()
            return
        }

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                _currentlyPlayingPath.value = filePath
                setOnCompletionListener {
                    _currentlyPlayingPath.value = null
                    onCompletion()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Failed to play audio: ${e.message}", e)
            stop()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _currentlyPlayingPath.value = null
    }

    fun isPlaying(filePath: String): Boolean {
        return _currentlyPlayingPath.value == filePath
    }
}
