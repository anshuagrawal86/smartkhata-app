package com.smartkhata.app.ui.screens.contactledger

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartkhata.app.ui.components.TransactionItemCard
import com.smartkhata.app.ui.theme.*
import com.smartkhata.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactLedgerScreen(
    viewModel: ContactLedgerViewModel,
    onNavigateBack: () -> Unit,
    onAddNewEntryForContact: (contactId: Long) -> Unit
) {
    val context = LocalContext.current
    val contact by viewModel.contact.collectAsState()
    val entries by viewModel.entries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = contact?.name ?: "Ledger",
                        fontWeight = FontWeight.Bold,
                        color = SurfaceWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SurfaceWhite)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val contactName = contact?.name ?: "Friend"
                        val balance = contact?.netBalance ?: 0.0
                        val status = if (balance >= 0) "pending to be collected" else "to be paid"
                        val shareText = "SmartKhata Statement for $contactName:\nTotal balance: ₹${Math.abs(balance).toInt()} ($status).\nGenerated via SmartKhata AI."
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Ledger Statement"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = SurfaceWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { contact?.id?.let { onAddNewEntryForContact(it) } },
                containerColor = PrimaryBlue,
                contentColor = SurfaceWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            // Balance Card for Person
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val balance = contact?.netBalance ?: 0.0
                        val balanceLabel = if (balance >= 0) "They owe you (लेना है)" else "You owe them (देना है)"
                        val balanceColor = if (balance >= 0) GotGreen else GaveRed

                        Text(
                            text = balanceLabel,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = Formatters.formatCurrency(Math.abs(balance)),
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
                                    Formatters.formatCurrency(contact?.totalGave ?: 0.0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GaveRed
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Received (लिया)", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    Formatters.formatCurrency(contact?.totalGot ?: 0.0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GotGreen
                                )
                            }
                        }
                    }
                }
            }

            // Entries section header
            item {
                Text(
                    text = "Transaction History (${entries.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No entries recorded for this person", color = TextSecondary)
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    TransactionItemCard(
                        entry = entry,
                        onDelete = { viewModel.deleteEntry(entry) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
