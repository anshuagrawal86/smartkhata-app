package com.smartkhata.app.data.repository

import com.smartkhata.app.data.local.AppDatabase
import com.smartkhata.app.data.local.entity.ContactEntity
import com.smartkhata.app.data.local.entity.EntryEntity
import com.smartkhata.app.data.local.entity.ReminderEntity
import com.smartkhata.app.data.model.DashboardSummary
import com.smartkhata.app.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LedgerRepository(private val db: AppDatabase) {

    private val contactDao = db.contactDao()
    private val entryDao = db.entryDao()
    private val reminderDao = db.reminderDao()

    val allContactsFlow: Flow<List<ContactEntity>> = contactDao.getAllContactsFlow()
    val allEntriesFlow: Flow<List<EntryEntity>> = entryDao.getAllEntriesFlow()
    val pendingRemindersFlow: Flow<List<ReminderEntity>> = reminderDao.getPendingRemindersFlow()

    fun searchEntries(query: String): Flow<List<EntryEntity>> = entryDao.searchEntriesFlow(query)

    fun getEntriesForContact(contactId: Long): Flow<List<EntryEntity>> =
        entryDao.getEntriesByContactFlow(contactId)

    suspend fun getContactById(id: Long): ContactEntity? = withContext(Dispatchers.IO) {
        contactDao.getContactById(id)
    }

    suspend fun getOrCreateContact(name: String, phoneNumber: String? = null): ContactEntity = withContext(Dispatchers.IO) {
        val existing = contactDao.getContactByName(name)
        if (existing != null) {
            return@withContext existing
        }
        val newContact = ContactEntity(
            name = name.trim(),
            phoneNumber = phoneNumber?.trim()
        )
        val id = contactDao.insertContact(newContact)
        newContact.copy(id = id)
    }

    suspend fun saveEntry(
        contactName: String,
        amount: Double,
        type: TransactionType,
        rawText: String,
        entryDate: Long = System.currentTimeMillis(),
        dueDate: Long? = null,
        mediaType: com.smartkhata.app.data.model.MediaType = com.smartkhata.app.data.model.MediaType.TEXT,
        mediaPath: String? = null,
        notes: String = ""
    ): Long = withContext(Dispatchers.IO) {
        // 1. Get or create contact
        val contact = getOrCreateContact(contactName)

        // 2. Insert Entry
        val entry = EntryEntity(
            contactId = contact.id,
            contactName = contact.name,
            rawText = rawText,
            transactionType = type,
            amount = amount,
            entryDate = entryDate,
            dueDate = dueDate,
            mediaType = mediaType,
            mediaPath = mediaPath,
            notes = notes
        )
        val entryId = entryDao.insertEntry(entry)

        // 3. Recalculate contact balances
        recalculateContactBalance(contact.id)

        // 4. Auto-create reminder if dueDate is specified
        if (dueDate != null && dueDate > System.currentTimeMillis()) {
            val reminder = ReminderEntity(
                entryId = entryId,
                contactId = contact.id,
                contactName = contact.name,
                remindAt = dueDate,
                amount = amount,
                notes = "Collect/Pay ₹${amount.toInt()} with ${contact.name}"
            )
            reminderDao.insertReminder(reminder)
        }

        entryId
    }

    suspend fun deleteEntry(entry: EntryEntity) = withContext(Dispatchers.IO) {
        entryDao.deleteEntry(entry)
        recalculateContactBalance(entry.contactId)
    }

    suspend fun updateEntry(
        entryId: Long,
        contactName: String,
        amount: Double,
        type: TransactionType,
        entryDate: Long,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val oldEntry = entryDao.getEntryById(entryId) ?: return@withContext
        val newContact = getOrCreateContact(contactName)
        val updated = oldEntry.copy(
            contactId = newContact.id,
            contactName = newContact.name,
            amount = amount,
            transactionType = type,
            entryDate = entryDate,
            notes = notes
        )
        entryDao.updateEntry(updated)
        recalculateContactBalance(newContact.id)
        if (oldEntry.contactId != newContact.id) {
            recalculateContactBalance(oldEntry.contactId)
        }
    }

    suspend fun recalculateContactBalance(contactId: Long) = withContext(Dispatchers.IO) {
        val contact = contactDao.getContactById(contactId) ?: return@withContext
        val entries = entryDao.getEntriesByContact(contactId)

        var gaveTotal = 0.0
        var gotTotal = 0.0
        var lastTimestamp = contact.createdTimestamp

        for (e in entries) {
            when (e.transactionType) {
                TransactionType.GAVE -> gaveTotal += e.amount
                TransactionType.GOT -> gotTotal += e.amount
                TransactionType.NOTE -> { /* no balance impact */ }
            }
            if (e.entryDate > lastTimestamp) {
                lastTimestamp = e.entryDate
            }
        }

        val net = gaveTotal - gotTotal // positive: they owe you; negative: you owe them
        contactDao.updateContact(
            contact.copy(
                totalGave = gaveTotal,
                totalGot = gotTotal,
                netBalance = net,
                lastTransactionTimestamp = lastTimestamp
            )
        )
    }

    fun getDashboardSummaryFlow(): Flow<DashboardSummary> {
        return contactDao.getAllContactsFlow().map { contacts ->
            var totalGave = 0.0
            var totalGot = 0.0
            for (c in contacts) {
                totalGave += c.totalGave
                totalGot += c.totalGot
            }
            DashboardSummary(
                totalGave = totalGave,
                totalGot = totalGot,
                netBalance = totalGave - totalGot,
                totalContacts = contacts.size
            )
        }
    }

    suspend fun createReminder(contactId: Long, contactName: String, remindAt: Long, notes: String, amount: Double = 0.0): Long = withContext(Dispatchers.IO) {
        reminderDao.insertReminder(
            ReminderEntity(
                contactId = contactId,
                contactName = contactName,
                remindAt = remindAt,
                notes = notes,
                amount = amount
            )
        )
    }

    suspend fun setReminderCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        reminderDao.setCompleted(id, completed)
    }

    suspend fun deleteReminder(reminder: ReminderEntity) = withContext(Dispatchers.IO) {
        reminderDao.deleteReminder(reminder)
    }
}
