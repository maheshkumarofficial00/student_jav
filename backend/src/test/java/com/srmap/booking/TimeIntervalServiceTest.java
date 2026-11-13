package com.srmap.booking;

import com.srmap.booking.service.TimeIntervalService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeIntervalServiceTest {

    private final TimeIntervalService service = new TimeIntervalService();

    @Test
    void testToMinAndToTime() {
        assertEquals(0, service.toMin("00:00"));
        assertEquals(600, service.toMin("10:00"));
        assertEquals(870, service.toMin("14:30"));
        assertEquals("10:00", service.toTime(600));
        assertEquals("14:30", service.toTime(870));
    }

    @Test
    void testSubtract() {
        List<TimeIntervalService.TimeRange> windows = List.of(
                new TimeIntervalService.TimeRange("09:00", "13:00"),
                new TimeIntervalService.TimeRange("14:00", "17:00")
        );
        List<TimeIntervalService.TimeRange> busy = List.of(
                new TimeIntervalService.TimeRange("10:00", "11:00")
        );

        List<TimeIntervalService.TimeRange> free = service.subtract(windows, busy);
        assertEquals(3, free.size());
        assertEquals("09:00", free.get(0).start());
        assertEquals("10:00", free.get(0).end());
        assertEquals("11:00", free.get(1).start());
        assertEquals("13:00", free.get(1).end());
        assertEquals("14:00", free.get(2).start());
        assertEquals("17:00", free.get(2).end());
    }

    @Test
    void testIntersect() {
        List<TimeIntervalService.TimeRange> a = List.of(
                new TimeIntervalService.TimeRange("09:00", "12:00"),
                new TimeIntervalService.TimeRange("14:00", "17:00")
        );
        List<TimeIntervalService.TimeRange> b = List.of(
                new TimeIntervalService.TimeRange("10:00", "15:00")
        );

        List<TimeIntervalService.TimeRange> result = service.intersect(a, b);
        assertEquals(2, result.size());
        assertEquals("10:00", result.get(0).start());
        assertEquals("12:00", result.get(0).end());
        assertEquals("14:00", result.get(1).start());
        assertEquals("15:00", result.get(1).end());
    }

    @Test
    void testIsFreeSlot() {
        List<TimeIntervalService.TimeRange> free = List.of(
                new TimeIntervalService.TimeRange("10:00", "12:00")
        );
        assertTrue(service.isFreeSlot(free, "10:00", "11:00"));
        assertTrue(service.isFreeSlot(free, "10:30", "11:30"));
        assertTrue(service.isFreeSlot(free, "10:00", "12:00"));
        assertFalse(service.isFreeSlot(free, "09:30", "10:30"));
        assertFalse(service.isFreeSlot(free, "11:30", "12:30"));
    }

    @Test
    void testOverlaps() {
        assertTrue(service.overlaps("10:00", "11:00", "10:30", "11:30"));
        // Back to back does not overlap
        assertFalse(service.overlaps("10:00", "11:00", "11:00", "12:00"));
        assertFalse(service.overlaps("11:00", "12:00", "10:00", "11:00"));
    }
}
