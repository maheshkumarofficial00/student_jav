package com.srmap.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * A one-off blocked time slot on a faculty member's calendar
 * (e.g. conference, leave, exam duty).
 */
@Entity
@Table(name = "schedule_blocks")
public class ScheduleBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private User faculty;

    @Column(nullable = false)
    private LocalDate date;

    /** HH:mm */
    @Column(nullable = false, length = 5)
    private String startTime;

    /** HH:mm */
    @Column(nullable = false, length = 5)
    private String endTime;

    @Column(length = 150)
    private String reason;

    public ScheduleBlock() {
    }

    public ScheduleBlock(Long id, User faculty, LocalDate date, String startTime, String endTime, String reason) {
        this.id = id;
        this.faculty = faculty;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private User faculty;
        private LocalDate date;
        private String startTime;
        private String endTime;
        private String reason;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder faculty(User faculty) { this.faculty = faculty; return this; }
        public Builder date(LocalDate date) { this.date = date; return this; }
        public Builder startTime(String startTime) { this.startTime = startTime; return this; }
        public Builder endTime(String endTime) { this.endTime = endTime; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }

        public ScheduleBlock build() {
            return new ScheduleBlock(id, faculty, date, startTime, endTime, reason);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getFaculty() { return faculty; }
    public void setFaculty(User faculty) { this.faculty = faculty; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
