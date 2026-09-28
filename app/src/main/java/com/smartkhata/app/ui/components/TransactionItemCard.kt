package com.smartkhata.app.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.smartkhata.app.data.local.entity.EntryEntity
import com.smartkhata.app.data.model.MediaType
import com.smartkhata.app.data.model.TransactionType
import com.smartkhata.app.ui.theme.*
import com.smartkhata.app.util.AudioPlayerHelper
import com.smartkhata.app.util.Formatters
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItemCard(
    entry: EntryEntity,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onEdit: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentPlayingPath by AudioPlayerHelper.currentlyPlayingPath.collectAsState()
    val isPlayingThis = currentPlayingPath != null && currentPlayingPath == entry.mediaPath

    val cardBg = if (isSelected) PrimaryBlue.copy(alpha = 0.08f) else SurfaceWhite
    val cardBorder = if (isSelected) PrimaryBlue else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .border(if (isSelected) 1.5.dp else 0.dp, cardBorder, RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onLongClick()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection Checkbox or Avatar
                if (isSelectionMode) {
                    IconButton(
                        onClick = onLongClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Select",
                            tint = if (isSelected) PrimaryBlue else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
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
                }

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

                        if (entry.mediaType == MediaType.AUDIO) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice note",
                                modifier = Modifier.size(15.dp),
                                tint = SecondaryTeal
                            )
                        } else if (entry.mediaType == MediaType.VIDEO) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video note",
                                modifier = Modifier.size(15.dp),
                                tint = PrimaryBlue
                            )
                        }
                    }

                    val displayNote = if (entry.notes.isNotBlank()) entry.notes else entry.rawText
                    if (displayNote.isNotBlank()) {
                        Text(
                            text = displayNote,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }

                    // Highly Visible, Prominent Date Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Formatters.formatDate(entry.entryDate),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }

                // Amount and Action Icons
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

                    if (!isSelectionMode) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit entry",
                                    tint = SecondaryTeal,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

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

            // Interactive Media Actions (Play Audio / Watch Video)
            if (!entry.mediaPath.isNullOrBlank()) {
                val mediaFile = File(entry.mediaPath)
                if (mediaFile.exists()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (entry.mediaType == MediaType.AUDIO) SecondaryTeal.copy(alpha = 0.08f)
                                else PrimaryBlue.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (entry.mediaType == MediaType.AUDIO) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = SecondaryTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPlayingThis) "Voice Note (Playing...)" else "Voice Note (${mediaFile.length() / 1024} KB)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SecondaryTeal
                                )
                            }

                            TextButton(
                                onClick = {
                                    if (isPlayingThis) {
                                        AudioPlayerHelper.stop()
                                    } else {
                                        AudioPlayerHelper.play(entry.mediaPath)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThis) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = SecondaryTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlayingThis) "Pause" else "Play", fontSize = 12.sp, color = SecondaryTeal, fontWeight = FontWeight.Bold)
                            }
                        } else if (entry.mediaType == MediaType.VIDEO) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Video Note (${mediaFile.length() / (1024 * 1024)} MB)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryBlue
                                )
                            }

                            TextButton(
                                onClick = {
                                    try {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", mediaFile)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "video/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open video: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Watch", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
