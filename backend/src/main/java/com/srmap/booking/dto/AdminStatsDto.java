package com.srmap.booking.dto;

public class AdminStatsDto {
    private long activeUsers;
    private long facultyCount;
    private long studentCount;
    private long rooms;
    private long resources;
    private long pendingAppointments;
    private long upcomingAppointments;
    private long upcomingReservations;

    public AdminStatsDto() {}

    public AdminStatsDto(long activeUsers, long facultyCount, long studentCount, long rooms,
                         long resources, long pendingAppointments, long upcomingAppointments,
                         long upcomingReservations) {
        this.activeUsers = activeUsers;
        this.facultyCount = facultyCount;
        this.studentCount = studentCount;
        this.rooms = rooms;
        this.resources = resources;
        this.pendingAppointments = pendingAppointments;
        this.upcomingAppointments = upcomingAppointments;
        this.upcomingReservations = upcomingReservations;
    }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getFacultyCount() { return facultyCount; }
    public void setFacultyCount(long facultyCount) { this.facultyCount = facultyCount; }

    public long getStudentCount() { return studentCount; }
    public void setStudentCount(long studentCount) { this.studentCount = studentCount; }

    public long getRooms() { return rooms; }
    public void setRooms(long rooms) { this.rooms = rooms; }

    public long getResources() { return resources; }
    public void setResources(long resources) { this.resources = resources; }

    public long getPendingAppointments() { return pendingAppointments; }
    public void setPendingAppointments(long pendingAppointments) { this.pendingAppointments = pendingAppointments; }

    public long getUpcomingAppointments() { return upcomingAppointments; }
    public void setUpcomingAppointments(long upcomingAppointments) { this.upcomingAppointments = upcomingAppointments; }

    public long getUpcomingReservations() { return upcomingReservations; }
    public void setUpcomingReservations(long upcomingReservations) { this.upcomingReservations = upcomingReservations; }
}
