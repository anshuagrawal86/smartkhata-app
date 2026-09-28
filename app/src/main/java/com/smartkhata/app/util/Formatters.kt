package com.smartkhata.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

    private val inrFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
    }

    fun formatCurrency(amount: Double): String {
        return inrFormat.format(amount).replace("INR", "₹").trim()
    }

    fun formatDate(epochMs: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = epochMs }

        val isToday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        return when {
            isToday -> "Today, ${timeFormat.format(Date(epochMs))}"
            isYesterday -> "Yesterday, ${timeFormat.format(Date(epochMs))}"
            else -> SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(epochMs))
        }
    }

    fun formatShortDate(epochMs: Long): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMs))
    }
}
