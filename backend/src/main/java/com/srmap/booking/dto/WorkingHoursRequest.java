package com.srmap.booking.dto;

import java.util.List;
import java.util.Map;

/** Request body for PUT /api/faculty/{id}/schedule/working-hours */
public class WorkingHoursRequest {
    /** Key = day-of-week (0-6 as strings or ints), value = list of {start, end} ranges */
    private Map<String, List<TimeRangeDto>> workingHours;

    public WorkingHoursRequest() {}

    public WorkingHoursRequest(Map<String, List<TimeRangeDto>> workingHours) {
        this.workingHours = workingHours;
    }

    public Map<String, List<TimeRangeDto>> getWorkingHours() { return workingHours; }
    public void setWorkingHours(Map<String, List<TimeRangeDto>> workingHours) { this.workingHours = workingHours; }

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
}
