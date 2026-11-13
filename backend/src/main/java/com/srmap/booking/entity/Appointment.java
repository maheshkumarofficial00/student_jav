package com.srmap.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * An appointment request from a student to a faculty member.
 * Optionally associated with a room.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private User faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false)
    private LocalDate date;

    /** HH:mm */
    @Column(nullable = false, length = 5)
    private String startTime;

    /** HH:mm */
    @Column(nullable = false, length = 5)
    private String endTime;

    @Column(nullable = false, length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Status status;

    public enum Status {
        PENDING, CONFIRMED, DECLINED, CANCELLED
    }

    public Appointment() {
    }

    public Appointment(Long id, User student, User faculty, Room room, LocalDate date,
                       String startTime, String endTime, String purpose, Status status) {
        this.id = id;
        this.student = student;
        this.faculty = faculty;
        this.room = room;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private User student;
        private User faculty;
        private Room room;
        private LocalDate date;
        private String startTime;
        private String endTime;
        private String purpose;
        private Status status;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder student(User student) { this.student = student; return this; }
        public Builder faculty(User faculty) { this.faculty = faculty; return this; }
        public Builder room(Room room) { this.room = room; return this; }
        public Builder date(LocalDate date) { this.date = date; return this; }
        public Builder startTime(String startTime) { this.startTime = startTime; return this; }
        public Builder endTime(String endTime) { this.endTime = endTime; return this; }
        public Builder purpose(String purpose) { this.purpose = purpose; return this; }
        public Builder status(Status status) { this.status = status; return this; }

        public Appointment build() {
            return new Appointment(id, student, faculty, room, date, startTime, endTime, purpose, status);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public User getFaculty() { return faculty; }
    public void setFaculty(User faculty) { this.faculty = faculty; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
