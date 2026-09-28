package com.smartkhata.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smartkhata.app.data.model.MediaType
import com.smartkhata.app.data.model.TransactionType

@Entity(
    tableName = "contacts",
    indices = [Index(value = ["name"], unique = true)]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String? = null,
    val totalGave: Double = 0.0,
    val totalGot: Double = 0.0,
    val netBalance: Double = 0.0, // positive = they owe you, negative = you owe them
    val lastTransactionTimestamp: Long = System.currentTimeMillis(),
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["contactId"]),
        Index(value = ["contactName"]),
        Index(value = ["entryDate"]),
        Index(value = ["transactionType"])
    ]
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val contactName: String,
    val rawText: String,
    val transactionType: TransactionType,
    val amount: Double,
    val currency: String = "INR",
    val entryDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val mediaType: MediaType = MediaType.TEXT,
    val mediaPath: String? = null,
    val tagsJson: String = "[]",
    val notes: String = "",
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["contactId"]),
        Index(value = ["remindAt"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: Long? = null,
    val contactId: Long,
    val contactName: String,
    val remindAt: Long,
    val notes: String,
    val amount: Double = 0.0,
    val isCompleted: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)
