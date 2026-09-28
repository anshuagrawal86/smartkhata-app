package com.smartkhata.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartkhata.app.data.local.entity.EntryEntity
import com.smartkhata.app.data.model.MediaType
import com.smartkhata.app.data.model.TransactionType
import com.smartkhata.app.ui.theme.*
import com.smartkhata.app.util.Formatters

@Composable
fun TransactionItemCard(
    entry: EntryEntity,
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initial
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when (entry.transactionType) {
                            TransactionType.GAVE -> GaveRedBg
                            TransactionType.GOT -> GotGreenBg
                            TransactionType.NOTE -> Color(0xFFE2E8F0)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.contactName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = when (entry.transactionType) {
                        TransactionType.GAVE -> GaveRed
                        TransactionType.GOT -> GotGreen
                        TransactionType.NOTE -> TextPrimary
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.contactName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Media badge
                    if (entry.mediaType == MediaType.AUDIO) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice note",
                            modifier = Modifier.size(16.dp),
                            tint = SecondaryTeal
                        )
                    } else if (entry.mediaType == MediaType.VIDEO) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video note",
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryBlue
                        )
                    }
                }

                if (entry.notes.isNotBlank() || entry.rawText.isNotBlank()) {
                    val displayNote = if (entry.notes.isNotBlank()) entry.notes else entry.rawText
                    Text(
                        text = displayNote,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }

                Text(
                    text = Formatters.formatDate(entry.entryDate),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Amount and Action
            Column(horizontalAlignment = Alignment.End) {
                val amountText = when (entry.transactionType) {
                    TransactionType.GAVE -> "- ${Formatters.formatCurrency(entry.amount)}"
                    TransactionType.GOT -> "+ ${Formatters.formatCurrency(entry.amount)}"
                    TransactionType.NOTE -> "Memo"
                }

                val amountColor = when (entry.transactionType) {
                    TransactionType.GAVE -> GaveRed
                    TransactionType.GOT -> GotGreen
                    TransactionType.NOTE -> TextSecondary
                }

                Text(
                    text = amountText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = amountColor
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
