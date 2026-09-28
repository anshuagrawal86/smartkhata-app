package com.smartkhata.app.ui.screens.contactledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartkhata.app.data.local.entity.ContactEntity
import com.smartkhata.app.data.local.entity.EntryEntity
import com.smartkhata.app.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactLedgerViewModel(
    private val contactId: Long,
    private val repository: LedgerRepository
) : ViewModel() {

    val contact = MutableStateFlow<ContactEntity?>(null)

    val entries: StateFlow<List<EntryEntity>> = repository.getEntriesForContact(contactId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadContact()
    }

    private fun loadContact() {
        viewModelScope.launch {
            contact.value = repository.getContactById(contactId)
        }
    }

    fun deleteEntry(entry: EntryEntity) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
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
