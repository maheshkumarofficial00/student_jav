package com.srmap.booking.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    // Faculty-only fields
    @Column(length = 150)
    private String department;

    @Column(length = 100)
    private String office;

    @Column(length = 100)
    private String title;

    // Faculty subjects – stored as comma-separated string; exposed as List via service
    @Column(length = 500)
    private String subjects;

    // Bidirectional relationships for faculty
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkingHour> workingHours = new ArrayList<>();

    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleBlock> scheduleBlocks = new ArrayList<>();

    public enum Role {
        STUDENT, FACULTY, ADMIN
    }

    public User() {
    }

    public User(Long id, String name, String email, String password, Role role, boolean active,
                String department, String office, String title, String subjects,
                List<WorkingHour> workingHours, List<ScheduleBlock> scheduleBlocks) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = active;
        this.department = department;
        this.office = office;
        this.title = title;
        this.subjects = subjects;
        if (workingHours != null) this.workingHours = workingHours;
        if (scheduleBlocks != null) this.scheduleBlocks = scheduleBlocks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String email;
        private String password;
        private Role role;
        private boolean active = true;
        private String department;
        private String office;
        private String title;
        private String subjects;
        private List<WorkingHour> workingHours = new ArrayList<>();
        private List<ScheduleBlock> scheduleBlocks = new ArrayList<>();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder role(Role role) { this.role = role; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder department(String department) { this.department = department; return this; }
        public Builder office(String office) { this.office = office; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder subjects(String subjects) { this.subjects = subjects; return this; }
        public Builder workingHours(List<WorkingHour> workingHours) { if (workingHours != null) this.workingHours = workingHours; return this; }
        public Builder scheduleBlocks(List<ScheduleBlock> scheduleBlocks) { if (scheduleBlocks != null) this.scheduleBlocks = scheduleBlocks; return this; }

        public User build() {
            return new User(id, name, email, password, role, active, department, office, title, subjects, workingHours, scheduleBlocks);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getOffice() { return office; }
    public void setOffice(String office) { this.office = office; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubjects() { return subjects; }
    public void setSubjects(String subjects) { this.subjects = subjects; }

    public List<WorkingHour> getWorkingHours() { return workingHours; }
    public void setWorkingHours(List<WorkingHour> workingHours) { this.workingHours = workingHours; }

    public List<ScheduleBlock> getScheduleBlocks() { return scheduleBlocks; }
    public void setScheduleBlocks(List<ScheduleBlock> scheduleBlocks) { this.scheduleBlocks = scheduleBlocks; }
}
