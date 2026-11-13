package com.srmap.booking.dto;

import java.util.List;

/**
 * Availability response for faculty, room, and resource endpoints.
 * Mirrors the mock-server format: { date, windows, busy, free }
 */
public class AvailabilityResponse {
    private String date;
    private List<TimeRange> windows;
    private List<BusySlot> busy;
    private List<TimeRange> free;

    public AvailabilityResponse() {}

    public AvailabilityResponse(String date, List<TimeRange> windows, List<BusySlot> busy, List<TimeRange> free) {
        this.date = date;
        this.windows = windows;
        this.busy = busy;
        this.free = free;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<TimeRange> getWindows() { return windows; }
    public void setWindows(List<TimeRange> windows) { this.windows = windows; }

    public List<BusySlot> getBusy() { return busy; }
    public void setBusy(List<BusySlot> busy) { this.busy = busy; }

    public List<TimeRange> getFree() { return free; }
    public void setFree(List<TimeRange> free) { this.free = free; }

    public static class TimeRange {
        private String start;
        private String end;

        public TimeRange() {}

        public TimeRange(String start, String end) {
            this.start = start;
            this.end = end;
        }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }

    public static class BusySlot {
        private String start;
        private String end;
        private String label;

        public BusySlot() {}

        public BusySlot(String start, String end, String label) {
            this.start = start;
            this.end = end;
            this.label = label;
        }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}
