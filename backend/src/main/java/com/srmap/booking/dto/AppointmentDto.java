package com.srmap.booking.dto;

import com.srmap.booking.entity.Appointment;

public class AppointmentDto {
    private Long id;
    private Long studentId;
    private Long facultyId;
    private Long roomId;
    private String date;
    private String start;
    private String end;
    private String purpose;
    private String status;
    private String studentName;
    private String facultyName;
    private String roomName;
    private String roomLocation;

    public AppointmentDto() {}

    public static AppointmentDto from(Appointment a) {
        if (a == null) return null;
        AppointmentDto d = new AppointmentDto();
        d.id = a.getId();
        d.studentId = a.getStudent() != null ? a.getStudent().getId() : null;
        d.facultyId = a.getFaculty() != null ? a.getFaculty().getId() : null;
        d.roomId = a.getRoom() != null ? a.getRoom().getId() : null;
        d.date = a.getDate() != null ? a.getDate().toString() : null;
        d.start = a.getStartTime();
        d.end = a.getEndTime();
        d.purpose = a.getPurpose();
        d.status = a.getStatus() != null ? a.getStatus().name() : null;
        d.studentName = a.getStudent() != null ? a.getStudent().getName() : null;
        d.facultyName = a.getFaculty() != null ? a.getFaculty().getName() : null;
        d.roomName = a.getRoom() != null ? a.getRoom().getName() : null;
        d.roomLocation = a.getRoom() != null ? a.getRoom().getBuilding() : null;
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomLocation() { return roomLocation; }
    public void setRoomLocation(String roomLocation) { this.roomLocation = roomLocation; }
}
