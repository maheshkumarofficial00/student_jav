package com.srmap.booking.dto;

/** Request body for POST /api/faculty/{id}/schedule/blocks */
public class ScheduleBlockRequest {
    private String date;
    private String start;
    private String end;
    private String reason;

    public ScheduleBlockRequest() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }

    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
