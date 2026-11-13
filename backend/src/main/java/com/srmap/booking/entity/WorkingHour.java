package com.srmap.booking.entity;

import jakarta.persistence.*;

/**
 * Represents one time-range on a specific day-of-week (0=Sunday … 6=Saturday)
 * for a faculty member's available working hours.
 */
@Entity
@Table(name = "working_hours")
public class WorkingHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private User faculty;

    /** Day of week: 0 = Sunday, 1 = Monday, …, 6 = Saturday */
    @Column(nullable = false)
    private int dayOfWeek;

    /** HH:mm */
    @Column(nullable = false, length = 5)
    private String startTime;

    /** HH:mm */
    @Column(nullable = false, length = 5)
    private String endTime;

    public WorkingHour() {
    }

    public WorkingHour(Long id, User faculty, int dayOfWeek, String startTime, String endTime) {
        this.id = id;
        this.faculty = faculty;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private User faculty;
        private int dayOfWeek;
        private String startTime;
        private String endTime;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder faculty(User faculty) { this.faculty = faculty; return this; }
        public Builder dayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; return this; }
        public Builder startTime(String startTime) { this.startTime = startTime; return this; }
        public Builder endTime(String endTime) { this.endTime = endTime; return this; }

        public WorkingHour build() {
            return new WorkingHour(id, faculty, dayOfWeek, startTime, endTime);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getFaculty() { return faculty; }
    public void setFaculty(User faculty) { this.faculty = faculty; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
