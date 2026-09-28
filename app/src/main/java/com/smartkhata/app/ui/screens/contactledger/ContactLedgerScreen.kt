package com.smartkhata.app.ui.screens.contactledger

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartkhata.app.data.local.entity.EntryEntity
import com.smartkhata.app.data.model.TransactionType
import com.smartkhata.app.ui.components.SelectedSummaryBottomDock
import com.smartkhata.app.ui.components.TransactionItemCard
import com.smartkhata.app.ui.theme.*
import com.smartkhata.app.util.Formatters
import com.smartkhata.app.util.StatementExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactLedgerScreen(
    viewModel: ContactLedgerViewModel,
    onNavigateBack: () -> Unit,
    onAddNewEntryForContact: (contactId: Long) -> Unit
) {
    val context = LocalContext.current
    val contact by viewModel.contact.collectAsState()
    val entries by viewModel.filteredEntries.collectAsState()
    val activeFilter by viewModel.dateFilter.collectAsState()
    val selectedIds by viewModel.selectedEntryIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    // Filtered Period Totals
    var periodGave by remember { mutableDoubleStateOf(0.0) }
    var periodGot by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(entries) {
        var g = 0.0
        var r = 0.0
        for (e in entries) {
            when (e.transactionType) {
                TransactionType.GAVE -> g += e.amount
                TransactionType.GOT -> r += e.amount
                TransactionType.NOTE -> {}
            }
        }
        periodGave = g
        periodGot = r
    }

    // Selected Items Totals
    val selectedEntries = remember(entries, selectedIds) {
        entries.filter { selectedIds.contains(it.id) }
    }
    val selectedGave = selectedEntries.filter { it.transactionType == TransactionType.GAVE }.sumOf { it.amount }
    val selectedGot = selectedEntries.filter { it.transactionType == TransactionType.GOT }.sumOf { it.amount }
    val selectedNet = selectedGave - selectedGot

    val partyName = contact?.name ?: "Contact"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = partyName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = SurfaceWhite
                        )
                        Text(
                            text = if (isSelectionMode) "${selectedIds.size} of ${entries.size} selected" else "${entries.size} entries (${activeFilter.label})",
                            fontSize = 12.sp,
                            color = SurfaceWhite.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            viewModel.clearSelection()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SurfaceWhite
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            if (selectedIds.size == entries.size) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.selectAll()
                            }
                        }) {
                            Icon(
                                if (selectedIds.size == entries.size) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = SurfaceWhite
                            )
                        }
                    } else {
                        // Quick CSV Download
                        IconButton(onClick = {
                            StatementExporter.shareCsvFile(context, partyName, entries)
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Download CSV", tint = SurfaceWhite)
                        }

                        // Quick WhatsApp Share
                        IconButton(onClick = {
                            val statement = StatementExporter.generateWhatsAppStatement(partyName, entries, activeFilter.label)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, statement)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Ledger Statement"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = SurfaceWhite)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        bottomBar = {
            if (isSelectionMode && selectedIds.isNotEmpty()) {
                SelectedSummaryBottomDock(
                    selectedCount = selectedIds.size,
                    selectedGave = selectedGave,
                    selectedGot = selectedGot,
                    selectedNet = selectedNet,
                    onExportCsv = {
                        StatementExporter.shareCsvFile(context, "$partyName (Selected)", selectedEntries)
                    },
                    onExportWhatsApp = {
                        val statement = StatementExporter.generateWhatsAppStatement(
                            partyName = "$partyName (Selected)",
                            entries = selectedEntries,
                            filterLabel = "Selected ${selectedEntries.size} entries"
                        )
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, statement)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Selected Statement"))
                    },
                    onDeleteSelected = {
                        viewModel.deleteSelectedEntries()
                    },
                    onClearSelection = {
                        viewModel.clearSelection()
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { contact?.id?.let { onAddNewEntryForContact(it) } },
                    containerColor = PrimaryBlue,
                    contentColor = SurfaceWhite
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            // 1. Date Filter Carousel
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateFilter.values().forEach { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { viewModel.setDateFilter(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryBlue
                            )
                        )
                    }
                }
            }

            // 2. Dynamic Balance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val periodNet = periodGave - periodGot
                        val balanceLabel = if (periodNet >= 0) "They owe you in ${activeFilter.label} (लेना है)" else "You owe them in ${activeFilter.label} (देना है)"
                        val balanceColor = if (periodNet >= 0) GotGreen else GaveRed

                        Text(
                            text = balanceLabel,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = Formatters.formatCurrency(Math.abs(periodNet)),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = DividerColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Given (दिया)", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    Formatters.formatCurrency(periodGave),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GaveRed
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Received (लिया)", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    Formatters.formatCurrency(periodGot),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GotGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Download Report Action Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    StatementExporter.shareCsvFile(context, partyName, entries)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download CSV", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val statement = StatementExporter.generateWhatsAppStatement(partyName, entries, activeFilter.label)
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, statement)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Statement"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Report", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. Selection helper banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions (${entries.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )

                    TextButton(
                        onClick = {
                            if (isSelectionMode) viewModel.clearSelection() else viewModel.selectAll()
                        }
                    ) {
                        Text(if (isSelectionMode) "Cancel Select" else "Select Entries", fontSize = 13.sp)
                    }
                }
            }

            // 4. Entries List
            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No entries recorded for this filter period", color = TextSecondary)
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    TransactionItemCard(
                        entry = entry,
                        isSelected = selectedIds.contains(entry.id),
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            // View details
                        },
                        onLongClick = {
                            viewModel.toggleSelection(entry.id)
                        },
                        onDelete = { viewModel.deleteEntry(entry) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }
}
