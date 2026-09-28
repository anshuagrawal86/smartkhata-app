package com.smartkhata.app.data.parser

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.smartkhata.app.data.model.ParsedTransaction
import com.smartkhata.app.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiHinglishParser(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun getApiKey(): String? {
        val prefs = context.getSharedPreferences("smart_khata_settings", Context.MODE_PRIVATE)
        return prefs.getString("gemini_api_key", null)?.takeIf { it.isNotBlank() }
    }

    suspend fun parseText(text: String): ParsedTransaction = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey == null) {
            return@withContext LocalHinglishParser.parse(text)
        }

        val prompt = """
            Analyze this diary or financial ledger note written in English, Hindi, or Hinglish:
            "$text"

            Extract the financial information and respond ONLY in valid JSON matching this schema:
            {
               "personName": string or null (name of the person or party),
               "amount": number (positive amount in INR),
               "type": "GAVE" or "GOT" or "NOTE" (GAVE = user gave money/debit/lent; GOT = user received money/credit/borrowed),
               "dueDateDaysFromNow": number or null (e.g. 7 if next week, 1 if tomorrow),
               "notes": string (brief summary or reason, e.g. "for chai", "grocery")
            }
        """.trimIndent()

        try {
            val responseJson = callGeminiGenerate(apiKey, prompt)
            parseGeminiResponse(responseJson, text)
        } catch (e: Exception) {
            Log.e("GeminiParser", "API call failed, falling back to local parser", e)
            LocalHinglishParser.parse(text)
        }
    }

    suspend fun parseAudioFile(audioFile: File): ParsedTransaction = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey == null) {
            // Cannot do cloud multimodal without key; return empty or prompt user
            return@withContext ParsedTransaction(
                description = "Audio recorded (Configure free Gemini key for auto-transcription)"
            )
        }

        try {
            val bytes = audioFile.readBytes()
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val prompt = """
                Listen carefully to this voice note recorded in English, Hindi, or Hinglish.
                It is a diary or ledger entry about some money transaction, debt, or credit.
                Extract the details and output ONLY a JSON object:
                {
                   "personName": string or null,
                   "amount": number,
                   "type": "GAVE" or "GOT" or "NOTE",
                   "transcription": string (full speech-to-text in original language),
                   "dueDateDaysFromNow": number or null,
                   "notes": string
                }
            """.trimIndent()

            val requestJson = JsonObject().apply {
                val contents = com.google.gson.JsonArray().apply {
                    val contentObj = JsonObject().apply {
                        val parts = com.google.gson.JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("text", prompt)
                            })
                            add(JsonObject().apply {
                                val inlineData = JsonObject().apply {
                                    addProperty("mimeType", "audio/mp4")
                                    addProperty("data", base64Data)
                                }
                                add("inlineData", inlineData)
                            })
                        }
                        add("parts", parts)
                    }
                    add(contentObj)
                }
                add("contents", contents)
            }

            val responseJson = executeRequest(apiKey, requestJson)
            parseGeminiResponse(responseJson, "")
        } catch (e: Exception) {
            Log.e("GeminiParser", "Audio processing failed", e)
            ParsedTransaction(description = "Audio recorded: ${audioFile.name}")
        }
    }

    suspend fun parseVideoFile(videoFile: File): ParsedTransaction = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey == null) {
            return@withContext ParsedTransaction(
                description = "Video recorded (Configure free Gemini key for video ledger analysis)"
            )
        }

        try {
            // Read video bytes (limit to ~10MB for free tier inline transfer)
            val bytes = videoFile.readBytes()
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val prompt = """
                Analyze this video message. It may contain a person speaking about a loan/repayment/transaction, or showing a receipt/bill/agreement.
                Extract the ledger entry details and output ONLY JSON:
                {
                   "personName": string or null,
                   "amount": number,
                   "type": "GAVE" or "GOT" or "NOTE",
                   "transcription": string,
                   "dueDateDaysFromNow": number or null,
                   "notes": string
                }
            """.trimIndent()

            val requestJson = JsonObject().apply {
                val contents = com.google.gson.JsonArray().apply {
                    val contentObj = JsonObject().apply {
                        val parts = com.google.gson.JsonArray().apply {
                            add(JsonObject().apply { addProperty("text", prompt) })
                            add(JsonObject().apply {
                                val inlineData = JsonObject().apply {
                                    addProperty("mimeType", "video/mp4")
                                    addProperty("data", base64Data)
                                }
                                add("inlineData", inlineData)
                            })
                        }
                        add("parts", parts)
                    }
                    add(contentObj)
                }
                add("contents", contents)
            }

            val responseJson = executeRequest(apiKey, requestJson)
            parseGeminiResponse(responseJson, "")
        } catch (e: Exception) {
            Log.e("GeminiParser", "Video processing failed", e)
            ParsedTransaction(description = "Video recorded: ${videoFile.name}")
        }
    }

    private fun callGeminiGenerate(apiKey: String, textPrompt: String): String {
        val jsonPayload = JsonObject().apply {
            val contents = com.google.gson.JsonArray().apply {
                val contentObj = JsonObject().apply {
                    val parts = com.google.gson.JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", textPrompt) })
                    }
                    add("parts", parts)
                }
                add(contentObj)
            }
            add("contents", contents)
        }
        return executeRequest(apiKey, jsonPayload)
    }

    private fun executeRequest(apiKey: String, payload: JsonObject): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Gemini API error HTTP ${response.code}: ${response.body?.string()}")
            }
            return response.body?.string() ?: ""
        }
    }

    private fun parseGeminiResponse(rawApiJson: String, fallbackText: String): ParsedTransaction {
        val root = gson.fromJson(rawApiJson, JsonObject::class.java)
        val textContent = root.getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString ?: return LocalHinglishParser.parse(fallbackText)

        // Clean any markdown ```json ... ``` blocks
        val cleanedJson = textContent.replace("```json", "").replace("```", "").trim()
        val parsed = gson.fromJson(cleanedJson, JsonObject::class.java)

        val name = parsed.get("personName")?.takeIf { !it.isJsonNull }?.asString
        val amount = parsed.get("amount")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
        val typeStr = parsed.get("type")?.takeIf { !it.isJsonNull }?.asString ?: "GAVE"
        val type = when (typeStr.uppercase()) {
            "GOT" -> TransactionType.GOT
            "NOTE" -> TransactionType.NOTE
            else -> TransactionType.GAVE
        }
        val notes = parsed.get("notes")?.takeIf { !it.isJsonNull }?.asString ?: ""
        val transcription = parsed.get("transcription")?.takeIf { !it.isJsonNull }?.asString

        val dueDateDays = parsed.get("dueDateDaysFromNow")?.takeIf { !it.isJsonNull }?.asLong
        val dueDateMs = dueDateDays?.let { System.currentTimeMillis() + it * 24 * 60 * 60 * 1000L }

        return ParsedTransaction(
            personName = name,
            amount = amount,
            type = type,
            dueDateEpochMs = dueDateMs,
            description = transcription ?: notes
        )
    }
}
