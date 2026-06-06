package com.trainseat.app.data.network

data class SeatAvailability(
    val status: AvailabilityStatus,
    val count: Int,
    val rawString: String
)

enum class AvailabilityStatus { AVAILABLE, WAITLIST, REGRET, UNKNOWN }

object SeatAvailabilityParser {

    private val AVL_REGEX = Regex("""AVL\s+(\d+)""", RegexOption.IGNORE_CASE)
    private val WL_REGEX = Regex("""WL[-\s](\d+)""", RegexOption.IGNORE_CASE)
    private val REGRET_REGEX = Regex("""REGRET|NOT\s+AVL""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): SeatAvailability {
        val trimmed = raw.trim()

        AVL_REGEX.find(trimmed)?.let { match ->
            val count = match.groupValues[1].toIntOrNull() ?: 0
            return SeatAvailability(AvailabilityStatus.AVAILABLE, count, trimmed)
        }

        WL_REGEX.find(trimmed)?.let { match ->
            val count = match.groupValues[1].toIntOrNull() ?: 0
            return SeatAvailability(AvailabilityStatus.WAITLIST, -count, trimmed)
        }

        if (REGRET_REGEX.containsMatchIn(trimmed)) {
            return SeatAvailability(AvailabilityStatus.REGRET, 0, trimmed)
        }

        return SeatAvailability(AvailabilityStatus.UNKNOWN, 0, trimmed)
    }

    fun parseFromInt(count: Int): SeatAvailability {
        return if (count > 0) {
            SeatAvailability(AvailabilityStatus.AVAILABLE, count, "AVL $count")
        } else {
            SeatAvailability(AvailabilityStatus.UNKNOWN, 0, "UNKNOWN")
        }
    }

    fun shouldTriggerAlarm(availability: SeatAvailability, threshold: Int): Boolean {
        return availability.status == AvailabilityStatus.AVAILABLE && availability.count < threshold
    }
}
