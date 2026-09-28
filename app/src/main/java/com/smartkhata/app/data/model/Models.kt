package com.smartkhata.app.data.model

enum class TransactionType {
    GAVE,  // You gave money to someone (Debit / You will collect)
    GOT,   // You received money from someone (Credit / You will give)
    NOTE   // General diary/memo note with no balance impact
}

enum class MediaType {
    TEXT,
    AUDIO,
    VIDEO
}

data class ParsedTransaction(
    val personName: String? = null,
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.GAVE,
    val dateEpochMs: Long = System.currentTimeMillis(),
    val dueDateEpochMs: Long? = null,
    val description: String = "",
    val confidence: Float = 1.0f
)

data class DashboardSummary(
    val totalGave: Double = 0.0,
    val totalGot: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalContacts: Int = 0,
    val pendingRemindersCount: Int = 0
)

data class BackupManifest(
    val appVersion: String = "1.0.0",
    val backupTimestamp: Long = System.currentTimeMillis(),
    val totalContacts: Int = 0,
    val totalEntries: Int = 0,
    val totalReminders: Int = 0,
    val mediaFiles: List<String> = emptyList()
)
