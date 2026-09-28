package com.smartkhata.app.ui.screens.contactledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartkhata.app.data.local.entity.ContactEntity
import com.smartkhata.app.data.local.entity.EntryEntity
import com.smartkhata.app.data.model.TransactionType
import com.smartkhata.app.data.repository.LedgerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateFilter(val label: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_30_DAYS("Last 30 Days")
}

class ContactLedgerViewModel(
    private val contactId: Long,
    private val repository: LedgerRepository
) : ViewModel() {

    val contact = MutableStateFlow<ContactEntity?>(null)
    val dateFilter = MutableStateFlow(DateFilter.ALL_TIME)
    val selectedEntryIds = MutableStateFlow<Set<Long>>(emptySet())
    val isSelectionMode = MutableStateFlow(false)

    private val allEntriesFlow = repository.getEntriesForContact(contactId)

    val filteredEntries: StateFlow<List<EntryEntity>> = combine(
        allEntriesFlow,
        dateFilter
    ) { list, filter ->
        val now = Calendar.getInstance()
        when (filter) {
            DateFilter.ALL_TIME -> list
            DateFilter.THIS_MONTH -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                list.filter { it.entryDate >= startOfMonth }
            }
            DateFilter.LAST_MONTH -> {
                val startOfLastMonth = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                val endOfLastMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                list.filter { it.entryDate in startOfLastMonth until endOfLastMonth }
            }
            DateFilter.LAST_30_DAYS -> {
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
                list.filter { it.entryDate >= thirtyDaysAgo }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadContact()
    }

    private fun loadContact() {
        viewModelScope.launch {
            contact.value = repository.getContactById(contactId)
        }
    }

    fun setDateFilter(filter: DateFilter) {
        dateFilter.value = filter
        selectedEntryIds.value = emptySet()
    }

    fun toggleSelection(entryId: Long) {
        val current = selectedEntryIds.value.toMutableSet()
        if (current.contains(entryId)) {
            current.remove(entryId)
        } else {
            current.add(entryId)
        }
        selectedEntryIds.value = current
        isSelectionMode.value = current.isNotEmpty()
    }

    fun selectAll() {
        selectedEntryIds.value = filteredEntries.value.map { it.id }.toSet()
        isSelectionMode.value = true
    }

    fun clearSelection() {
        selectedEntryIds.value = emptySet()
        isSelectionMode.value = false
    }

    fun deleteEntry(entry: EntryEntity) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
            loadContact()
        }
    }

    fun deleteSelectedEntries() {
        viewModelScope.launch {
            val ids = selectedEntryIds.value
            val entriesToDelete = filteredEntries.value.filter { ids.contains(it.id) }
            for (e in entriesToDelete) {
                repository.deleteEntry(e)
            }
            clearSelection()
            loadContact()
        }
    }

    class Factory(
        private val contactId: Long,
        private val repository: LedgerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContactLedgerViewModel(contactId, repository) as T
        }
    }
}
