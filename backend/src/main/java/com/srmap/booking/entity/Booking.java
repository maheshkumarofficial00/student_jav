package com.srmap.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A room/resource reservation made by a student or faculty member.
 * Resources are stored as a many-to-many join table.
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "booking_resources",
        joinColumns = @JoinColumn(name = "booking_id"),
        inverseJoinColumns = @JoinColumn(name = "resource_id")
    )
    private List<Resource> resources = new ArrayList<>();

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
        CONFIRMED, CANCELLED
    }

    public Booking() {
    }

    public Booking(Long id, User user, Room room, List<Resource> resources, LocalDate date,
                   String startTime, String endTime, String purpose, Status status) {
        this.id = id;
        this.user = user;
        this.room = room;
        if (resources != null) this.resources = resources;
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
        private User user;
        private Room room;
        private List<Resource> resources = new ArrayList<>();
        private LocalDate date;
        private String startTime;
        private String endTime;
        private String purpose;
        private Status status;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder room(Room room) { this.room = room; return this; }
        public Builder resources(List<Resource> resources) { if (resources != null) this.resources = resources; return this; }
        public Builder date(LocalDate date) { this.date = date; return this; }
        public Builder startTime(String startTime) { this.startTime = startTime; return this; }
        public Builder endTime(String endTime) { this.endTime = endTime; return this; }
        public Builder purpose(String purpose) { this.purpose = purpose; return this; }
        public Builder status(Status status) { this.status = status; return this; }

        public Booking build() {
            return new Booking(id, user, room, resources, date, startTime, endTime, purpose, status);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public List<Resource> getResources() { return resources; }
    public void setResources(List<Resource> resources) { this.resources = resources; }

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
