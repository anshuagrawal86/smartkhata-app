package com.smartkhata.app.data.local.dao

import androidx.room.*
import com.smartkhata.app.data.local.entity.ContactEntity
import com.smartkhata.app.data.local.entity.EntryEntity
import com.smartkhata.app.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY lastTransactionTimestamp DESC")
    fun getAllContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY lastTransactionTimestamp DESC")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getContactByName(name: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY entryDate DESC, createdTimestamp DESC")
    fun getAllEntriesFlow(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries ORDER BY entryDate DESC, createdTimestamp DESC")
    suspend fun getAllEntries(): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE contactId = :contactId ORDER BY entryDate DESC, createdTimestamp DESC")
    fun getEntriesByContactFlow(contactId: Long): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE contactId = :contactId ORDER BY entryDate DESC, createdTimestamp DESC")
    suspend fun getEntriesByContact(contactId: Long): List<EntryEntity>

    @Query("""
        SELECT * FROM entries 
        WHERE (:query = '' OR contactName LIKE '%' || :query || '%' OR rawText LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY entryDate DESC
    """)
    fun searchEntriesFlow(query: String): Flow<List<EntryEntity>>

    @Query("""
        SELECT * FROM entries 
        WHERE entryDate >= :startDate AND entryDate <= :endDate
        ORDER BY entryDate DESC
    """)
    fun getEntriesByDateRangeFlow(startDate: Long, endDate: Long): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): EntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<EntryEntity>)

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Delete
    suspend fun deleteEntry(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE contactId = :contactId")
    suspend fun deleteEntriesForContact(contactId: Long)

    @Query("DELETE FROM entries")
    suspend fun deleteAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY remindAt ASC")
    fun getPendingRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY remindAt DESC")
    fun getAllRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun getAllReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
