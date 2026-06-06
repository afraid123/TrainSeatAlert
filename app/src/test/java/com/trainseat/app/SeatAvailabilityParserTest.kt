package com.trainseat.app

import com.trainseat.app.data.network.AvailabilityStatus
import com.trainseat.app.data.network.SeatAvailabilityParser
import org.junit.Assert.assertEquals
import org.junit.Test

class SeatAvailabilityParserTest {

    @Test
    fun `parses AVL status correctly`() {
        val result = SeatAvailabilityParser.parse("AVL 343")
        assertEquals(AvailabilityStatus.AVAILABLE, result.status)
        assertEquals(343, result.count)
    }

    @Test
    fun `parses WL status with space`() {
        val result = SeatAvailabilityParser.parse("WL 3")
        assertEquals(AvailabilityStatus.WAITLIST, result.status)
        assertEquals(-3, result.count)
    }

    @Test
    fun `parses WL status with dash`() {
        val result = SeatAvailabilityParser.parse("WL-5")
        assertEquals(AvailabilityStatus.WAITLIST, result.status)
        assertEquals(-5, result.count)
    }

    @Test
    fun `parses REGRET status`() {
        val result = SeatAvailabilityParser.parse("REGRET")
        assertEquals(AvailabilityStatus.REGRET, result.status)
        assertEquals(0, result.count)
    }

    @Test
    fun `parses NOT AVL status`() {
        val result = SeatAvailabilityParser.parse("NOT AVL")
        assertEquals(AvailabilityStatus.REGRET, result.status)
    }

    @Test
    fun `returns UNKNOWN for unrecognised input`() {
        val result = SeatAvailabilityParser.parse("SOME RANDOM TEXT")
        assertEquals(AvailabilityStatus.UNKNOWN, result.status)
        assertEquals(0, result.count)
    }

    @Test
    fun `alarm triggers when available and below threshold`() {
        val avail = SeatAvailabilityParser.parse("AVL 5")
        assert(SeatAvailabilityParser.shouldTriggerAlarm(avail, 10))
    }

    @Test
    fun `alarm does not trigger when above threshold`() {
        val avail = SeatAvailabilityParser.parse("AVL 50")
        assert(!SeatAvailabilityParser.shouldTriggerAlarm(avail, 10))
    }

    @Test
    fun `alarm does not trigger on WL even if count below threshold`() {
        val avail = SeatAvailabilityParser.parse("WL 3")
        assert(!SeatAvailabilityParser.shouldTriggerAlarm(avail, 10))
    }
}
