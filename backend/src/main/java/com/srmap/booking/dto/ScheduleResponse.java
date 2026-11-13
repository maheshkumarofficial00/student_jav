package com.srmap.booking.dto;

import java.util.List;
import java.util.Map;

/**
 * Schedule response: working hours indexed by day-of-week (0-6) + list of blocks.
 * Mirrors the mock-server format: { workingHours: { 0: [], 1: [{start,end},...], ...}, blocks: [...] }
 */
public class ScheduleResponse {
    /** Key = day of week (0=Sunday … 6=Saturday), value = list of time ranges for that day */
    private Map<Integer, List<TimeRangeDto>> workingHours;
    private List<ScheduleBlockDto> blocks;

    public ScheduleResponse() {}

    public ScheduleResponse(Map<Integer, List<TimeRangeDto>> workingHours, List<ScheduleBlockDto> blocks) {
        this.workingHours = workingHours;
        this.blocks = blocks;
    }

    public Map<Integer, List<TimeRangeDto>> getWorkingHours() { return workingHours; }
    public void setWorkingHours(Map<Integer, List<TimeRangeDto>> workingHours) { this.workingHours = workingHours; }

    public List<ScheduleBlockDto> getBlocks() { return blocks; }
    public void setBlocks(List<ScheduleBlockDto> blocks) { this.blocks = blocks; }

    public static class TimeRangeDto {
        private String start;
        private String end;

        public TimeRangeDto() {}

        public TimeRangeDto(String start, String end) {
            this.start = start;
            this.end = end;
        }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }

    public static class ScheduleBlockDto {
        private Long id;
        private String date;
        private String start;
        private String end;
        private String reason;

        public ScheduleBlockDto() {}

        public ScheduleBlockDto(Long id, String date, String start, String end, String reason) {
            this.id = id;
            this.date = date;
            this.start = start;
            this.end = end;
            this.reason = reason;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
