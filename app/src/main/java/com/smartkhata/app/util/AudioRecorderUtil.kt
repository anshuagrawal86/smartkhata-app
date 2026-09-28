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

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile.absolutePath)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Failed to start recording", e)
            }
        }
        return outputFile
    }

    fun stopRecording(): File? {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording", e)
        } finally {
            recorder = null
        }
        return currentOutputFile
    }

    fun getAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
