package com.smartkhata.app.ui.screens.reminders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartkhata.app.ui.theme.*
import com.smartkhata.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment & Ledger Reminders", fontWeight = FontWeight.Bold, color = SurfaceWhite) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = SurfaceWhite
            ) {
                Icon(Icons.Default.AddAlarm, contentDescription = "Add Reminder")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (reminders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No pending reminders",
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                fontSize = 16.sp
                            )
                            Text(
                                "Tap the alarm button below to set a reminder for a contact",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(reminders, key = { it.id }) { reminder ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.contactName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )

                                if (reminder.amount > 0) {
                                    Text(
                                        text = "Amount: ${Formatters.formatCurrency(reminder.amount)}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = GaveRed
                                    )
                                }

                                if (reminder.notes.isNotBlank()) {
                                    Text(
                                        text = reminder.notes,
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Remind at: ${Formatters.formatDate(reminder.remindAt)}",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                // Mark as done button
                                IconButton(onClick = { viewModel.completeReminder(reminder) }) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Complete",
                                        tint = GotGreen
                                    )
                                }

                                // Quick WhatsApp Share
                                IconButton(onClick = {
                                    val amt = if (reminder.amount > 0) "₹${reminder.amount.toInt()}" else "our pending amount"
                                    val msg = "Hi ${reminder.contactName}, a gentle reminder regarding $amt on SmartKhata."
                                    val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(msg)}")
                                    }
                                    context.startActivity(waIntent)
                                }) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Share on WhatsApp",
                                        tint = SecondaryTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add Reminder Dialog
        if (showAddDialog) {
            var selectedContactName by remember { mutableStateOf(contacts.firstOrNull()?.name ?: "") }
            var reminderAmount by remember { mutableStateOf("") }
            var reminderNotes by remember { mutableStateOf("") }
            var daysFromNow by remember { mutableFloatStateOf(1f) }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Set Payment Reminder", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = selectedContactName,
                            onValueChange = { selectedContactName = it },
                            label = { Text("Contact Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = reminderAmount,
                            onValueChange = { reminderAmount = it },
                            label = { Text("Amount (₹, optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = reminderNotes,
                            onValueChange = { reminderNotes = it },
                            label = { Text("Notes / Reason") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Remind in: ${daysFromNow.toInt()} day(s)",
                            fontSize = 13.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = daysFromNow,
                            onValueChange = { daysFromNow = it },
                            valueRange = 1f..30f,
                            steps = 28
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val contactId = contacts.find { it.name.equals(selectedContactName, ignoreCase = true) }?.id ?: 0L
                            val remindTime = System.currentTimeMillis() + (daysFromNow.toLong() * 24 * 60 * 60 * 1000L)
                            viewModel.createReminder(
                                contactId = contactId,
                                contactName = selectedContactName.ifBlank { "Contact" },
                                amount = reminderAmount.toDoubleOrNull() ?: 0.0,
                                notes = reminderNotes,
                                remindAtEpochMs = remindTime
                            )
                            showAddDialog = false
                        }
                    ) {
                        Text("Schedule Reminder")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
