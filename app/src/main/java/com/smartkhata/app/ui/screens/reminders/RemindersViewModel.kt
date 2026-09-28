package com.smartkhata.app.ui.screens.reminders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartkhata.app.data.local.entity.ContactEntity
import com.smartkhata.app.data.local.entity.ReminderEntity
import com.smartkhata.app.data.repository.LedgerRepository
import com.smartkhata.app.reminders.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersViewModel(
    application: Application,
    private val repository: LedgerRepository
) : AndroidViewModel(application) {

    private val scheduler = ReminderScheduler(application)

    val reminders: StateFlow<List<ReminderEntity>> = repository.pendingRemindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactEntity>> = repository.allContactsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createReminder(contactId: Long, contactName: String, amount: Double, notes: String, remindAtEpochMs: Long) {
        viewModelScope.launch {
            val id = repository.createReminder(contactId, contactName, remindAtEpochMs, notes, amount)
            scheduler.scheduleReminder(id, contactName, amount, notes, remindAtEpochMs)
        }
    }

    fun completeReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.setReminderCompleted(reminder.id, true)
            scheduler.cancelReminder(reminder.id)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            scheduler.cancelReminder(reminder.id)
        }
    }

    class Factory(
        private val application: Application,
        private val repository: LedgerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RemindersViewModel(application, repository) as T
        }
    }
}
