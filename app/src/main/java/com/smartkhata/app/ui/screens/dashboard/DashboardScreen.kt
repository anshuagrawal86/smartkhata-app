package com.smartkhata.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartkhata.app.ui.components.TransactionItemCard
import com.smartkhata.app.ui.theme.*
import com.smartkhata.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToNewEntry: (mode: String) -> Unit,
    onNavigateToContactLedger: (contactId: Long) -> Unit
) {
    val summary by viewModel.dashboardSummary.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.filter.collectAsState()

    var showFabMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SmartKhata AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = SurfaceWhite
                        )
                        Text(
                            text = "Smart Multilingual Ledger (खाता)",
                            fontSize = 12.sp,
                            color = SurfaceWhite.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showFabMenu) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            onNavigateToNewEntry("video")
                        },
                        icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                        text = { Text("Video Note") },
                        containerColor = SecondaryTeal,
                        contentColor = SurfaceWhite
                    )

                    ExtendedFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            onNavigateToNewEntry("voice")
                        },
                        icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                        text = { Text("Voice Note (बोलकर)") },
                        containerColor = PrimaryBlue,
                        contentColor = SurfaceWhite
                    )

                    ExtendedFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            onNavigateToNewEntry("type")
                        },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        text = { Text("Type Entry (लिखकर)") },
                        containerColor = PrimaryBlueDark,
                        contentColor = SurfaceWhite
                    )
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = if (showFabMenu) GaveRed else PrimaryBlue,
                    contentColor = SurfaceWhite
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "New Entry"
                    )
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
            // 1. Balance Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Net Balance",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )

                        val netColor = if (summary.netBalance >= 0) GotGreen else GaveRed
                        val netSign = if (summary.netBalance >= 0) "+" else ""

                        Text(
                            text = "$netSign${Formatters.formatCurrency(summary.netBalance)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = netColor,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DividerColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // You gave (to collect)
                            Column {
                                Text(
                                    text = "You'll Get (दिया)",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = Formatters.formatCurrency(summary.totalGave),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GaveRed
                                )
                            }

                            // You got (to pay)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "You'll Give (लिया)",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = Formatters.formatCurrency(summary.totalGot),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GotGreen
                                )
                            }
                        }
                    }
                }
            }

            // 2. Search Box
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search by name, note, or amount…", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DividerColor
                    )
                )
            }

            // 3. Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == FilterType.ALL,
                        onClick = { viewModel.setFilter(FilterType.ALL) },
                        label = { Text("All Entries") }
                    )
                    FilterChip(
                        selected = activeFilter == FilterType.YOU_GAVE,
                        onClick = { viewModel.setFilter(FilterType.YOU_GAVE) },
                        label = { Text("You Gave") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GaveRedBg,
                            selectedLabelColor = GaveRed
                        )
                    )
                    FilterChip(
                        selected = activeFilter == FilterType.YOU_GOT,
                        onClick = { viewModel.setFilter(FilterType.YOU_GOT) },
                        label = { Text("You Got") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GotGreenBg,
                            selectedLabelColor = GotGreen
                        )
                    )
                }
            }

            // 4. Section Header
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
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
            }

            // 5. Entries List
            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No entries found",
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Tap + to add a voice, text, or video entry",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    TransactionItemCard(
                        entry = entry,
                        onClick = { onNavigateToContactLedger(entry.contactId) },
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
