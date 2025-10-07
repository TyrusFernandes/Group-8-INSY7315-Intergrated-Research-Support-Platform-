package com.example.acadenceapp.util

import java.util.*
import kotlin.math.abs

object ForYouUtils {

    /** "ai,  ML ; data-science" -> ["ai","ml","data-science"] */
    fun parseTags(input: String): List<String> =
        input.split(',', ';', '|')
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotEmpty() }
            .distinct()

    /** Given a list of tag lists (one per document), return up to [limit] unique tags, most frequent first. */
    fun dedupeTopTags(allDocTags: List<List<String>>, limit: Int = 10): List<String> {
        val counts = mutableMapOf<String, Int>()
        for (tags in allDocTags) {
            // count each tag once per document
            tags.toSet().forEach { t -> counts[t] = (counts[t] ?: 0) + 1 }
        }
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
            .take(limit)
    }

    /** Map review status to an ARGB color int (same logic used in RequestCardAdapter). */
    fun statusColorFor(status: String): Int = when (status.lowercase(Locale.getDefault())) {
        "approved", "done", "completed" -> 0xFF2FD732.toInt() // green
        "rejected" -> 0xFFCC2828.toInt()                      // red
        "in progress", "reviewing" -> 0xFF6A1B9A.toInt()      // purple
        else -> 0xFF0066FF.toInt()                            // default blue
    }

    /** Build a dates line like: "Due: 07/10/2025   •   Created: 01/10/2025" */
    fun formatDates(due: Date?, created: Date?): String {
        val df = java.text.SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        val dueTxt = due?.let(df::format) ?: "—"
        val createdTxt = created?.let(df::format).orEmpty()
        return if (createdTxt.isNotEmpty()) "Due: $dueTxt   •   Created: $createdTxt" else "Due: $dueTxt"
    }

    /** Build the Firestore payload for a review request (kept pure for testing). */
    fun buildRequestPayload(
        docId: String,
        docTitle: String,
        assignedToUid: String,
        assignedToName: String,
        reviewType: String,
        urgency: String,
        visibility: String,
        notes: String,
        nowProvider: () -> Date = { Date() }
    ): Map<String, Any> {
        val now = nowProvider()
        return mapOf(
            "docId" to docId,
            "docTitle" to docTitle,
            "assignedToUid" to assignedToUid,
            "assignedToName" to assignedToName,
            "reviewType" to reviewType,
            "urgency" to urgency,
            "visibility" to visibility,
            "reviewDetails" to notes,
            // your app stores Firestore Timestamp, but for unit tests we'll pass Date; the calling code can wrap to Timestamp
            "dueDate" to now,
            "status" to "Pending",
            "createdAt" to now
        )
    }

    /** Convenience for verifying two dates are "near" each other in tests (milliseconds). */
    fun nearNow(target: Date, toleranceMs: Long = 1_500L, now: Date = Date()): Boolean =
        abs(now.time - target.time) <= toleranceMs
}
