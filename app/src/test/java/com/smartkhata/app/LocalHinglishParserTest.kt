package com.smartkhata.app

import com.smartkhata.app.data.model.TransactionType
import com.smartkhata.app.data.parser.LocalHinglishParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocalHinglishParserTest {

    @Test
    fun testHinglishGaveEntry() {
        val input = "Maine Ramesh ko 500 diye kal chai ke"
        val result = LocalHinglishParser.parse(input)

        assertEquals("Ramesh", result.personName)
        assertEquals(500.0, result.amount, 0.01)
        assertEquals(TransactionType.GAVE, result.type)
        println("Parsed Hinglish Gave: $result")
    }

    @Test
    fun testHinglishGotEntry() {
        val input = "Ramesh ne 200 diya wapas"
        val result = LocalHinglishParser.parse(input)

        assertEquals("Ramesh", result.personName)
        assertEquals(200.0, result.amount, 0.01)
        assertEquals(TransactionType.GOT, result.type)
        println("Parsed Hinglish Got: $result")
    }

    @Test
    fun testEnglishGaveEntry() {
        val input = "Paid 1200 to Suresh for groceries"
        val result = LocalHinglishParser.parse(input)

        assertEquals("Suresh", result.personName)
        assertEquals(1200.0, result.amount, 0.01)
        assertEquals(TransactionType.GAVE, result.type)
        println("Parsed English Gave: $result")
    }

    @Test
    fun testHindiNumericWord() {
        val input = "Pooja ko paanch sau diye"
        val result = LocalHinglishParser.parse(input)

        assertEquals("Pooja", result.personName)
        assertEquals(500.0, result.amount, 0.01)
        assertEquals(TransactionType.GAVE, result.type)
        println("Parsed Hindi Word Number: $result")
    }

    @Test
    fun testDueDatePicker() {
        val input = "Anita se 2000 lena hai agle hafte"
        val result = LocalHinglishParser.parse(input)

        assertEquals("Anita", result.personName)
        assertEquals(2000.0, result.amount, 0.01)
        assertNotNull(result.dueDateEpochMs)
        println("Parsed Due Date: $result")
    }
}
