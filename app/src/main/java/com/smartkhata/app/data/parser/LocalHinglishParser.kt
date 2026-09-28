package com.smartkhata.app.data.parser

import com.smartkhata.app.data.model.ParsedTransaction
import com.smartkhata.app.data.model.TransactionType
import java.util.Calendar
import java.util.regex.Pattern

object LocalHinglishParser {

    private val GAVE_PATTERNS = listOf(
        "diye", "diya", "de diya", "pay kiya", "send kiya", "bheje", "bheja",
        "de diye", "gave", "paid", "sent", "lent", "given", "give", "transferred",
        "dena pada", "se lena hai", "ko udhar diya"
    )

    private val GOT_PATTERNS = listOf(
        "mila", "mile", "aaye", "aaya", "liya", "le liya", "receive hua",
        "receive kiya", "got", "received", "borrowed", "collected", "took",
        "credited", "ne diya", "ne de diya", "waapas kiya", "wapas mila"
    )

    private val HINDI_NUMBERS = mapOf(
        "ek" to 1.0, "do" to 2.0, "teen" to 3.0, "chaar" to 4.0, "paanch" to 5.0,
        "chhe" to 6.0, "saat" to 7.0, "aath" to 8.0, "nau" to 9.0, "das" to 10.0,
        "gyarah" to 11.0, "barah" to 12.0, "terah" to 13.0, "chaudah" to 14.0, "pandrah" to 15.0,
        "solah" to 16.0, "satrah" to 17.0, "atharah" to 18.0, "unnees" to 19.0, "bees" to 20.0,
        "pachees" to 25.0, "tees" to 30.0, "chalis" to 40.0, "pachas" to 50.0, "paanch sau" to 500.0,
        "sau" to 100.0, "dedh sau" to 150.0, "dhai sau" to 250.0, "hazaar" to 1000.0, "hazar" to 1000.0,
        "lakh" to 100000.0
    )

    fun parse(input: String): ParsedTransaction {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedTransaction()
        }

        val amount = extractAmount(trimmed)
        val type = extractType(trimmed)
        val personName = extractPersonName(trimmed)
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
        // 1. Look for numeric patterns like ₹500, 500rs, 1,200.50, 500/-
        val regex = Regex("""(?i)(?:rs\.?|inr|₹)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)\s*(?:rs\.?|rupaye|rupay|rupees|/-)?""")
        val matches = regex.findAll(text)

        for (match in matches) {
            val numStr = match.groups[1]?.value?.replace(",", "")
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0) {
                return parsed
            }
        }

        // 2. Check Hindi / Hinglish colloquial words (longer phrases first)
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

        // Explicit "ne diya" -> The other person gave, so user GOT
        if (lower.contains("ne diya") || lower.contains("ne de diya") || lower.contains("se mila") || lower.contains("from")) {
            return TransactionType.GOT
        }

        // Check Got patterns
        for (pattern in GOT_PATTERNS) {
            if (lower.contains(pattern)) {
                return TransactionType.GOT
            }
        }

        // Check Gave patterns
        for (pattern in GAVE_PATTERNS) {
            if (lower.contains(pattern)) {
                return TransactionType.GAVE
            }
        }

        // Default to GAVE if an amount was spent/recorded
        return TransactionType.GAVE
    }

    private fun extractPersonName(text: String): String? {
        val lower = text.trim()

        // 1. Hinglish pattern: "Name ko ...", "Name se ...", "Name ne ..."
        val hinglishPattern = Pattern.compile("""(?i)\b([A-Z\u0900-\u097F][a-zA-Z\u0900-\u097F]{2,20})\s+(?:ko|se|ne|ka|ke)\b""")
        val m1 = hinglishPattern.matcher(text)
        if (m1.find()) {
            val candidate = m1.group(1)?.trim()
            if (!isExcludedWord(candidate)) {
                return candidate?.capitalizeFirst()
            }
        }

        // 2. English patterns: "paid to Name", "gave to Name", "from Name", "to Name"
        val englishPattern = Pattern.compile("""(?i)\b(?:to|from|for|paid|gave)\s+([A-Z][a-zA-Z]{2,20})\b""")
        val m2 = englishPattern.matcher(text)
        if (m2.find()) {
            val candidate = m2.group(1)?.trim()
            if (!isExcludedWord(candidate)) {
                return candidate?.capitalizeFirst()
            }
        }

        // 3. Fallback: First capitalized word that is not a stop word
        val words = text.split(" ", "\n", "\t")
        for (word in words) {
            val clean = word.replace(Regex("""[^a-zA-Z\u0900-\u097F]"""), "")
            if (clean.isNotEmpty() && clean[0].isUpperCase() && clean.length >= 3 && !isExcludedWord(clean)) {
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
            // default is today
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
            clean = clean.replace(person, "", ignoreCase = true)
        }
        if (amount > 0) {
            clean = clean.replace(amount.toInt().toString(), "")
        }
        val stopWords = listOf("ko", "se", "ne", "diye", "diya", "mila", "mile", "paid", "gave", "got", "to", "from", "for", "rs", "inr", "rupaye", "rupees", "kal", "aaj")
        for (sw in stopWords) {
            clean = clean.replace("\\b$sw\\b".toRegex(RegexOption.IGNORE_CASE), "")
        }
        return clean.replace(Regex("""\s+"""), " ").trim()
    }

    private fun isExcludedWord(word: String?): Boolean {
        if (word == null) return true
        val excluded = setOf(
            "maine", "humne", "aaj", "kal", "parso", "subah", "shaam", "raat", "rupaye",
            "rupees", "paid", "gave", "cash", "online", "gpay", "phonepe", "paytm", "bank"
        )
        return excluded.contains(word.lowercase())
    }

    private fun String.capitalizeFirst(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
