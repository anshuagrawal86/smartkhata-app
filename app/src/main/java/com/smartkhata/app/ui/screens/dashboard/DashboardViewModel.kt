package com.smartkhata.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartkhata.app.data.local.entity.EntryEntity
import com.smartkhata.app.data.model.DashboardSummary
import com.smartkhata.app.data.model.TransactionType
import com.smartkhata.app.data.repository.LedgerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class FilterType {
    ALL,
    YOU_GAVE,
    YOU_GOT
}

class DashboardViewModel(private val repository: LedgerRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(FilterType.ALL)
    val filter = _filter.asStateFlow()

    val selectedEntryIds = MutableStateFlow<Set<Long>>(emptySet())
    val isSelectionMode = MutableStateFlow(false)

    val dashboardSummary: StateFlow<DashboardSummary> = repository.getDashboardSummaryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    val entries: StateFlow<List<EntryEntity>> = combine(
        repository.allEntriesFlow,
        _searchQuery,
        _filter
    ) { allEntries, query, selectedFilter ->
        allEntries.filter { entry ->
            val matchesQuery = query.isBlank() ||
                    entry.contactName.contains(query, ignoreCase = true) ||
                    entry.notes.contains(query, ignoreCase = true) ||
                    entry.rawText.contains(query, ignoreCase = true) ||
                    entry.amount.toString().contains(query)

            val matchesFilter = when (selectedFilter) {
                FilterType.ALL -> true
                FilterType.YOU_GAVE -> entry.transactionType == TransactionType.GAVE
                FilterType.YOU_GOT -> entry.transactionType == TransactionType.GOT
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        clearSelection()
    }

    fun setFilter(filterType: FilterType) {
        _filter.value = filterType
        clearSelection()
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
        selectedEntryIds.value = entries.value.map { it.id }.toSet()
        isSelectionMode.value = true
    }

    fun clearSelection() {
        selectedEntryIds.value = emptySet()
        isSelectionMode.value = false
    }

    fun updateEntry(
        entryId: Long,
        contactName: String,
        amount: Double,
        type: TransactionType,
        entryDate: Long,
        notes: String
    ) {
        viewModelScope.launch {
            repository.updateEntry(entryId, contactName, amount, type, entryDate, notes)
        }
    }

    fun deleteEntry(entry: EntryEntity) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    fun deleteSelectedEntries() {
        viewModelScope.launch {
            val ids = selectedEntryIds.value
            val entriesToDelete = entries.value.filter { ids.contains(it.id) }
            for (e in entriesToDelete) {
                repository.deleteEntry(e)
            }
            clearSelection()
        }
    }

    class Factory(private val repository: LedgerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
