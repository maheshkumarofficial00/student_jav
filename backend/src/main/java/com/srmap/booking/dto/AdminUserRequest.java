package com.srmap.booking.dto;

import java.util.List;

/** Request body for create/update admin user. */
public class AdminUserRequest {
    private String name;
    private String email;
    private String password;
    private String role;
    private Boolean active;
    // Faculty-only
    private String department;
    private String office;
    private String title;
    private List<String> subjects;

    public AdminUserRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getOffice() { return office; }
    public void setOffice(String office) { this.office = office; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
}
