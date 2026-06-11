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

    /**
     * Parses the RapidAPI "irctc1" checkSeatAvailability JSON response.
     * Response shape:
     * {
     *   "status": true,
     *   "data": [
     *     { "date": "dd-mm-yyyy", "current_status": "AVAILABLE-0024", ... },
     *     ...
     *   ]
     * }
     * current_status examples: "AVAILABLE-0024", "RAC 12", "GNWL/123", "REGRET", "NOT AVAILABLE"
     */
    fun parseIrctc1(json: String, targetDate: String): SeatAvailability {
        return try {
            val root = com.google.gson.Gson()
                .fromJson(json, com.google.gson.JsonObject::class.java)
                ?: return SeatAvailability(AvailabilityStatus.UNKNOWN, 0, "NO_DATA")
            val data = root.getAsJsonArray("data")
                ?: return SeatAvailability(AvailabilityStatus.UNKNOWN, 0, "NO_DATA")
            if (data.size() == 0) {
                return SeatAvailability(AvailabilityStatus.UNKNOWN, 0, "NO_DATA")
            }

            var chosen = data[0].asJsonObject
            for (el in data) {
                val obj = el.asJsonObject
                val d = obj.get("date")?.takeIf { !it.isJsonNull }?.asString ?: ""
                if (d == targetDate) {
                    chosen = obj
                    break
                }
            }

            val status = chosen.get("current_status")?.takeIf { !it.isJsonNull }?.asString
                ?: chosen.get("confirmTktStatus")?.takeIf { !it.isJsonNull }?.asString
                ?: "UNKNOWN"

            classifyIrctcStatus(status)
        } catch (e: Exception) {
            SeatAvailability(AvailabilityStatus.UNKNOWN, 0, "PARSE_ERROR")
        }
    }

    private fun classifyIrctcStatus(raw: String): SeatAvailability {
        val s = raw.uppercase().trim()
        val num = Regex("""(\d+)""").find(s)?.value?.toIntOrNull() ?: 0
        return when {
            s.contains("AVAILABLE") || s.startsWith("AVL") ->
                SeatAvailability(AvailabilityStatus.AVAILABLE, num, raw)
            s.contains("RAC") ->
                SeatAvailability(AvailabilityStatus.WAITLIST, -num, raw)
            s.contains("WL") ->
                SeatAvailability(AvailabilityStatus.WAITLIST, -num, raw)
            s.contains("REGRET") || s.contains("NOT AVAILABLE") ||
                    s.contains("DEPARTED") || s.contains("CHARTING") ->
                SeatAvailability(AvailabilityStatus.REGRET, 0, raw)
            else ->
                SeatAvailability(AvailabilityStatus.UNKNOWN, 0, raw)
        }
    }
}
