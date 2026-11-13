package com.srmap.booking.dto;

/** Request body for POST /api/appointments */
public class AppointmentRequest {
    private Long facultyId;
    private Long roomId;
    private String date;
    private String start;
    private String end;
    private String purpose;

    public AppointmentRequest() {}

    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }

    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
