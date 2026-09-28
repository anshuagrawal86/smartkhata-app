package com.smartkhata.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartkhata.app.ui.theme.*
import com.smartkhata.app.util.Formatters

@Composable
fun SelectedSummaryBottomDock(
    selectedCount: Int,
    selectedGave: Double,
    selectedGot: Double,
    selectedNet: Double,
    onExportCsv: () -> Unit,
    onExportWhatsApp: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearSelection: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        color = SurfaceWhite,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Count + Net calculation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$selectedCount Selected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Gave: ${Formatters.formatCurrency(selectedGave)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GaveRed
                        )
                        Text(
                            text = "Got: ${Formatters.formatCurrency(selectedGot)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GotGreen
                        )
                    }
                }

                val netColor = if (selectedNet >= 0) GotGreen else GaveRed
                val netSign = if (selectedNet >= 0) "+" else "-"
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Selected Net",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "$netSign${Formatters.formatCurrency(Math.abs(selectedNet))}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = netColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearSelection,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", fontSize = 13.sp)
                }

                Button(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV Report", fontSize = 13.sp)
                }

                Button(
                    onClick = onExportWhatsApp,
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Statement", fontSize = 13.sp)
                }

                IconButton(
                    onClick = onDeleteSelected,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete selected",
                        tint = GaveRed
                    )
                }
            }
        }
    }
}
