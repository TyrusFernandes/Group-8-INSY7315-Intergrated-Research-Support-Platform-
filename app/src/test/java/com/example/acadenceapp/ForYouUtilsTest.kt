package com.example.acadenceapp

import com.example.acadenceapp.util.ForYouUtils
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class ForYouUtilsTest {

    @Test
    fun parseTags_trimsLowercasesAndDedupes() {
        val input = " AI , ml ; Data-Science |  ml  ,"
        val out = ForYouUtils.parseTags(input)
        assertEquals(listOf("ai", "ml", "data-science"), out)
    }

    @Test
    fun dedupeTopTags_ordersByFrequency_thenAlphabetically() {
        val docs = listOf(
            listOf("ai", "ml", "vision"),
            listOf("ai", "nlp"),
            listOf("nlp", "ai"),
            listOf("data", "ml"),
        )
        val top = ForYouUtils.dedupeTopTags(docs, limit = 10)
        // ai(3), nlp(2), ml(2), data(1), vision(1) -> ties sorted by key
        assertEquals(listOf("ai", "ml", "nlp", "data", "vision").sortedWith(
            compareByDescending<String> { tag ->
                when (tag) {
                    "ai" -> 3; "nlp" -> 2; "ml" -> 2; "data" -> 1; "vision" -> 1
                    else -> 0
                }
            }.thenBy { it }
        ).take(5), top.take(5))
        // A simpler sanity check that "ai" is first and there are no duplicates:
        assertEquals("ai", top.first())
        assertEquals(top.size, top.toSet().size)
    }

    @Test
    fun statusColorFor_mapsStatuses() {
        assertEquals(0xFF2FD732.toInt(), ForYouUtils.statusColorFor("Approved"))
        assertEquals(0xFF2FD732.toInt(), ForYouUtils.statusColorFor("completed"))
        assertEquals(0xFFCC2828.toInt(), ForYouUtils.statusColorFor("rejected"))
        assertEquals(0xFF6A1B9A.toInt(), ForYouUtils.statusColorFor("In Progress"))
        assertEquals(0xFF0066FF.toInt(), ForYouUtils.statusColorFor("unknown-status"))
    }

    @Test
    fun formatDates_handlesNullCreated() {
        val cal = Calendar.getInstance()
        cal.set(2025, Calendar.OCTOBER, 7, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val line = ForYouUtils.formatDates(due = cal.time, created = null)
        assertTrue(line.startsWith("Due:"))
        assertFalse(line.contains("Created:"))
    }

    @Test
    fun buildRequestPayload_fillsExpectedKeys_andTimesNearNow() {
        val fixedNow = Date(1730908800000L) // any stable instant
        val payload = ForYouUtils.buildRequestPayload(
            docId = "abc",
            docTitle = "My Paper",
            assignedToUid = "u1",
            assignedToName = "Dr. Patel",
            reviewType = "Technical",
            urgency = "High",
            visibility = "Private",
            notes = "please check section 3",
            nowProvider = { fixedNow }
        )

        // keys present
        val expectedKeys = setOf(
            "docId","docTitle","assignedToUid","assignedToName",
            "reviewType","urgency","visibility","reviewDetails",
            "dueDate","status","createdAt"
        )
        assertTrue(payload.keys.containsAll(expectedKeys))
        assertEquals("Pending", payload["status"])
        assertEquals(fixedNow, payload["dueDate"])
        assertEquals(fixedNow, payload["createdAt"])
    }

    @Test
    fun nearNow_trueWithinTolerance() {
        val now = Date()
        val slightlyAfter = Date(now.time + 900)
        assertTrue(ForYouUtils.nearNow(slightlyAfter, toleranceMs = 1500, now = now))
    }

    @Test
    fun nearNow_falseOutsideTolerance() {
        val now = Date()
        val later = Date(now.time + 10_000)
        assertFalse(ForYouUtils.nearNow(later, toleranceMs = 1500, now = now))
    }
}
