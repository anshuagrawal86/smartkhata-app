package com.smartkhata.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.smartkhata.app.data.local.entity.EntryEntity
import com.smartkhata.app.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatementExporter {

    fun generateWhatsAppStatement(
        partyName: String,
        entries: List<EntryEntity>,
        filterLabel: String = "All Time"
    ): String {
        var totalGave = 0.0
        var totalGot = 0.0

        for (e in entries) {
            when (e.transactionType) {
                TransactionType.GAVE -> totalGave += e.amount
                TransactionType.GOT -> totalGot += e.amount
                TransactionType.NOTE -> {}
            }
        }
        val net = totalGave - totalGot
        val netStatus = if (net >= 0) "You'll Get (लेना है)" else "You'll Give (देना है)"

        val sb = StringBuilder()
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📊 SMARTKHATA STATEMENT")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("👤 Party: $partyName")
        sb.appendLine("📅 Period: $filterLabel")
        sb.appendLine("📝 Total Transactions: ${entries.size}")
        sb.appendLine("─────────────────────")
        sb.appendLine("🔴 Total Given (दिया): ${Formatters.formatCurrency(totalGave)}")
        sb.appendLine("🟢 Total Received (लिया): ${Formatters.formatCurrency(totalGot)}")
        sb.appendLine("─────────────────────")
        val netSign = if (net >= 0) "+" else "-"
        sb.appendLine("💰 NET BALANCE: $netSign${Formatters.formatCurrency(Math.abs(net))} ($netStatus)")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("Recent Line Items:")

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val displayEntries = entries.take(20)
        for (e in displayEntries) {
            val typeSymbol = when (e.transactionType) {
                TransactionType.GAVE -> "🔴 -"
                TransactionType.GOT -> "🟢 +"
                TransactionType.NOTE -> "📝"
            }
            val note = if (e.notes.isNotBlank()) " (${e.notes})" else ""
            sb.appendLine("• ${dateFormat.format(Date(e.entryDate))}: $typeSymbol${Formatters.formatCurrency(e.amount)}$note")
        }

        if (entries.size > 20) {
            sb.appendLine("... and ${entries.size - 20} more transactions.")
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("Generated via SmartKhata AI App")
        return sb.toString()
    }

    fun generateCsvContent(partyName: String, entries: List<EntryEntity>): String {
        val sb = StringBuilder()
        // CSV Header
        sb.appendLine("Date,Time,Party Name,Type,Amount (INR),Notes,Media Attached,Raw Input")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        for (e in entries) {
            val d = Date(e.entryDate)
            val dateStr = dateFormat.format(d)
            val timeStr = timeFormat.format(d)
            val typeStr = when (e.transactionType) {
                TransactionType.GAVE -> "GAVE (Debit)"
                TransactionType.GOT -> "GOT (Credit)"
                TransactionType.NOTE -> "NOTE"
            }
            val safeParty = escapeCsv(e.contactName)
            val safeNotes = escapeCsv(e.notes)
            val safeRaw = escapeCsv(e.rawText)
            val mediaType = e.mediaType.name

            sb.appendLine("$dateStr,$timeStr,$safeParty,$typeStr,${e.amount},$safeNotes,$mediaType,$safeRaw")
        }

        return sb.toString()
    }

    fun shareCsvFile(context: Context, partyName: String, entries: List<EntryEntity>) {
        val csvData = generateCsvContent(partyName, entries)
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeName = partyName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(exportDir, "SmartKhata_${safeName}_$timeStamp.csv")

        FileOutputStream(file).use { fos ->
            fos.write(csvData.toByteArray(Charsets.UTF_8))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "SmartKhata Statement - $partyName")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Download / Share Statement CSV"))
    }

    private fun escapeCsv(value: String): String {
        var res = value.replace("\"", "\"\"")
        if (res.contains(",") || res.contains("\n") || res.contains("\"")) {
            res = "\"$res\""
        }
        return res
    }
}
