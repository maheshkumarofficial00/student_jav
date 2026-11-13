package com.srmap.booking.dto;

import java.util.List;

/** Request body for POST /api/bookings */
public class BookingRequest {
    private Long roomId;
    private List<Long> resourceIds;
    private String date;
    private String start;
    private String end;
    private String purpose;

    public BookingRequest() {}

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public List<Long> getResourceIds() { return resourceIds; }
    public void setResourceIds(List<Long> resourceIds) { this.resourceIds = resourceIds; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }

    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
