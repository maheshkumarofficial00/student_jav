package com.srmap.booking.dto;

import com.srmap.booking.entity.User;
import java.util.Arrays;
import java.util.List;

/** Full user record for admin CRUD – no password, no schedule. */
public class AdminUserDto {
    private Long id;
    private String name;
    private String email;
    private String role;
    private boolean active;
    private String department;
    private String office;
    private String title;
    private List<String> subjects;

    public AdminUserDto() {}

    public static AdminUserDto from(User u) {
        if (u == null) return null;
        AdminUserDto d = new AdminUserDto();
        d.id = u.getId();
        d.name = u.getName();
        d.email = u.getEmail();
        d.role = u.getRole() != null ? u.getRole().name() : null;
        d.active = u.isActive();
        d.department = u.getDepartment();
        d.office = u.getOffice();
        d.title = u.getTitle();
        String raw = u.getSubjects();
        d.subjects = (raw == null || raw.isBlank())
            ? List.of()
            : Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getOffice() { return office; }
    public void setOffice(String office) { this.office = office; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
}
