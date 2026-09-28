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
    }

    fun setFilter(filterType: FilterType) {
        _filter.value = filterType
    }

    fun deleteEntry(entry: EntryEntity) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    class Factory(private val repository: LedgerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
