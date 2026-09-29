package com.smartkhata.app.data.parser

import com.smartkhata.app.data.model.ParsedTransaction
import com.smartkhata.app.data.model.TransactionType
import java.util.Calendar
import java.util.regex.Pattern

object LocalHinglishParser {

    private val STOP_WORDS = setOf(
        "ko", "se", "ne", "ka", "ke", "ki", "to", "from", "for", "in", "on", "at",
        "rs", "inr", "rupaye", "rupay", "rupees", "cash", "gpay", "phonepe", "paytm",
        "online", "bank", "transfer", "diye", "diya", "de", "de diya", "pay",
        "paid", "gave", "given", "give", "lent", "bheja", "bheje", "send", "sent", "mila", "mile",
        "aaye", "aaya", "liya", "le", "got", "received", "receive", "borrowed", "collected",
        "kal", "aaj", "parso", "today", "yesterday", "tomorrow", "maine", "humne", "mene",
        "hai", "tha", "the", "thi", "hua", "karo", "karna", "dena", "lena", "udhar",
        "me", "i", "we", "he", "she", "you"
    )

    private val HINDI_NUMBERS = mapOf(
        "paanch sau" to 500.0,
        "dedh sau" to 150.0,
        "dhai sau" to 250.0,
        "dhai hazaar" to 2500.0,
        "dedh hazaar" to 1500.0,
        "do hazaar" to 2000.0,
        "teen hazaar" to 3000.0,
        "paanch hazaar" to 5000.0,
        "das hazaar" to 10000.0,
        "ek lakh" to 100000.0,
        "do lakh" to 200000.0,
        "hazaar" to 1000.0,
        "hazar" to 1000.0,
        "lakh" to 100000.0,
        "sau" to 100.0,
        "pachas" to 50.0,
        "pachees" to 25.0,
        "tees" to 30.0,
        "chalis" to 40.0,
        "bees" to 20.0,
        "unnees" to 19.0,
        "atharah" to 18.0,
        "satrah" to 17.0,
        "solah" to 16.0,
        "pandrah" to 15.0,
        "chaudah" to 14.0,
        "terah" to 13.0,
        "barah" to 12.0,
        "gyarah" to 11.0,
        "das" to 10.0,
        "nau" to 9.0,
        "aath" to 8.0,
        "saat" to 7.0,
        "chhe" to 6.0,
        "paanch" to 5.0,
        "chaar" to 4.0,
        "teen" to 3.0,
        "do" to 2.0,
        "ek" to 1.0
    )

    fun parse(input: String): ParsedTransaction {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedTransaction()
        }

        val amount = extractAmount(trimmed)
        val type = extractType(trimmed)
        val personName = extractPersonName(trimmed, amount)
        val dateEpochMs = extractDate(trimmed)
        val dueDateEpochMs = extractDueDate(trimmed)
        val notes = extractNotes(trimmed, personName, amount)

        return ParsedTransaction(
            personName = personName,
            amount = amount,
            type = type,
            dateEpochMs = dateEpochMs,
            dueDateEpochMs = dueDateEpochMs,
            description = notes
        )
    }

    private fun extractAmount(text: String): Double {
        // 1. Numeric patterns (₹500, 500rs, 1,200.50, 500/-, 500)
        val regex = Regex("""(?i)(?:rs\.?|inr|₹)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)\s*(?:rs\.?|rupaye|rupay|rupees|/-)?""")
        val matches = regex.findAll(text)

        for (match in matches) {
            val numStr = match.groups[1]?.value?.replace(",", "")
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0) {
                return parsed
            }
        }

        // 2. Hindi / Hinglish colloquial words (longer phrases evaluated first)
        val lower = text.lowercase()
        val sortedEntries = HINDI_NUMBERS.entries.sortedByDescending { it.key.length }
        for ((word, value) in sortedEntries) {
            if (lower.contains("\\b$word\\b".toRegex())) {
                return value
            }
        }

        return 0.0
    }

    private fun extractType(text: String): TransactionType {
        val lower = text.lowercase()

        // "ne diya" -> Other person gave to user -> user GOT (credit)
        if (lower.contains("ne diya") || lower.contains("ne de diya") || lower.contains("se mila") || lower.contains("se liya") || lower.contains("from ")) {
            return TransactionType.GOT
        }

        // Keywords indicating receipt
        val gotWords = listOf("mila", "mile", "aaye", "aaya", "liya", "le liya", "got", "received", "receive hua", "receive kiya", "borrowed", "collected")
        for (w in gotWords) {
            if (lower.contains(w)) return TransactionType.GOT
        }

        // Keywords indicating payment / giving
        val gaveWords = listOf("diye", "diya", "de diya", "pay kiya", "send kiya", "bheja", "bheje", "gave", "paid", "sent", "lent", "given", "give", "transferred", "to ")
        for (w in gaveWords) {
            if (lower.contains(w)) return TransactionType.GAVE
        }

        // Default to GAVE if money moved
        return TransactionType.GAVE
    }

    private fun extractPersonName(text: String, amount: Double): String? {
        val trimmed = text.trim()

        // Pattern 1: Hinglish "Name ko", "Name se", "Name ne"
        val p1 = Pattern.compile("""(?i)\b([a-zA-Z\u0900-\u097F]{2,25})\s+(?:ko|se|ne|ka|ke)\b""")
        val m1 = p1.matcher(trimmed)
        if (m1.find()) {
            val candidate = m1.group(1)?.trim()
            if (!isExcludedWord(candidate)) {
                return candidate?.capitalizeFirst()
            }
        }

        // Pattern 2: Subject before action verb: "Anita gave", "Ramesh paid", "Anita sent"
        val p2 = Pattern.compile("""(?i)\b([a-zA-Z\u0900-\u097F]{2,25})\s+(?:gave|paid|sent|lent|diya|diye)\b""")
        val m2 = p2.matcher(trimmed)
        if (m2.find()) {
            val candidate = m2.group(1)?.trim()
            if (!isExcludedWord(candidate)) {
                return candidate?.capitalizeFirst()
            }
        }

        // Pattern 3: English prepositions "to Name", "from Name", "paid to Name", "gave to Name"
        val p3 = Pattern.compile("""(?i)\b(?:to|from|paid to|gave to|received from)\s+([a-zA-Z\u0900-\u097F]{2,25})\b""")
        val m3 = p3.matcher(trimmed)
        if (m3.find()) {
            val candidate = m3.group(1)?.trim()
            if (!isExcludedWord(candidate)) {
                return candidate?.capitalizeFirst()
            }
        }

        // Pattern 4: Fallback token scan (handles "Ramesh 500", "500 Ramesh", "Rohan 350/-", "350 rohan")
        val words = trimmed.split(Regex("""[\s,]+"""))
        for (w in words) {
            val clean = w.replace(Regex("""[^a-zA-Z\u0900-\u097F]"""), "")
            if (clean.length >= 2 && !isExcludedWord(clean)) {
                return clean.capitalizeFirst()
            }
        }

        return null
    }

    private fun extractDate(text: String): Long {
        val lower = text.lowercase()
        val calendar = Calendar.getInstance()

        when {
            lower.contains("kal") && (lower.contains("diye") || lower.contains("mila") || lower.contains("aaye") || lower.contains("yesterday")) -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            lower.contains("yesterday") -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            lower.contains("parso") || lower.contains("day before yesterday") -> {
                calendar.add(Calendar.DAY_OF_YEAR, -2)
            }
        }
        return calendar.timeInMillis
    }

    private fun extractDueDate(text: String): Long? {
        val lower = text.lowercase()
        val calendar = Calendar.getInstance()

        return when {
            lower.contains("agle hafte") || lower.contains("next week") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 7)
                calendar.timeInMillis
            }
            lower.contains("kal tak") || lower.contains("kal dena") || lower.contains("tomorrow") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.timeInMillis
            }
            lower.contains("agle mahine") || lower.contains("next month") -> {
                calendar.add(Calendar.MONTH, 1)
                calendar.timeInMillis
            }
            else -> null
        }
    }

    private fun extractNotes(text: String, person: String?, amount: Double): String {
        var clean = text
        if (person != null) {
            clean = clean.replace(Regex("""(?i)\b${Regex.escape(person)}\b"""), " ")
        }
        if (amount > 0) {
            val intPart = amount.toInt().toString()
            clean = clean.replace(Regex("""(?i)(?:rs\.?|inr|₹)?\s*$intPart(?:\.00?)?\s*(?:rs\.?|rupaye|rupay|rupees|/-)?"""), " ")
        }
        clean = clean.replace(Regex("""(?i)\b(?:diye|diya|de diya|mila|mile|liya|le liya|paid|gave|got|received|udhar)\b"""), " ")
        clean = clean.replace(Regex("""(?i)\b(?:ko|se|ne)\b"""), " ")
        clean = clean.replace(Regex("""[\s,;]+"""), " ").trim()
        return if (clean.isNotBlank()) clean.capitalizeFirst() else text.trim()
    }

    private fun isExcludedWord(word: String?): Boolean {
        if (word == null) return true
        val lower = word.lowercase()
        return STOP_WORDS.contains(lower) || lower.all { it.isDigit() }
    }

    private fun String.capitalizeFirst(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
