package com.srmap.booking.dto;

import java.util.List;

/** Response for POST /api/availability/combined */
public class CombinedAvailabilityResponse {
    private int durationMinutes;
    private List<CombinedWindow> windows;

    public CombinedAvailabilityResponse() {}

    public CombinedAvailabilityResponse(int durationMinutes, List<CombinedWindow> windows) {
        this.durationMinutes = durationMinutes;
        this.windows = windows;
    }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public List<CombinedWindow> getWindows() { return windows; }
    public void setWindows(List<CombinedWindow> windows) { this.windows = windows; }

    public static class CombinedWindow {
        private String date;
        private String start;
        private String end;
        private Long roomId;
        private String roomName;
        private String roomLocation;
        private Integer capacity;

        public CombinedWindow() {}

        public CombinedWindow(String date, String start, String end, Long roomId,
                              String roomName, String roomLocation, Integer capacity) {
            this.date = date;
            this.start = start;
            this.end = end;
            this.roomId = roomId;
            this.roomName = roomName;
            this.roomLocation = roomLocation;
            this.capacity = capacity;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }

        public Long getRoomId() { return roomId; }
        public void setRoomId(Long roomId) { this.roomId = roomId; }

        public String getRoomName() { return roomName; }
        public void setRoomName(String roomName) { this.roomName = roomName; }

        public String getRoomLocation() { return roomLocation; }
        public void setRoomLocation(String roomLocation) { this.roomLocation = roomLocation; }

        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }
    }
}
