package com.smartkhata.app.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorderUtil(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    fun startRecording(): File {
        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(mediaDir, "AUDIO_$timeStamp.m4a")
        currentOutputFile = outputFile

        try {
            stopRecording()

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128000)
            mr.setAudioSamplingRate(44100)
            mr.setOutputFile(outputFile.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording: ${e.message}", e)
            try {
                recorder?.reset()
                recorder?.release()
            } catch (_: Exception) {}
            recorder = null
            throw e
        }
        return outputFile
    }

    fun stopRecording(): File? {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.w("AudioRecorder", "Warning stopping recorder: ${e.message}")
        } finally {
            try {
                recorder?.release()
            } catch (_: Exception) {}
            recorder = null
        }
        return currentOutputFile?.takeIf { it.exists() && it.length() > 0 }
    }

    fun getAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
