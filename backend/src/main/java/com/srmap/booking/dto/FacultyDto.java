package com.srmap.booking.dto;

import com.srmap.booking.entity.User;
import java.util.Arrays;
import java.util.List;

/** Faculty public profile (what students and other roles see). */
public class FacultyDto {
    private Long id;
    private String name;
    private String email;
    private String department;
    private String office;
    private String title;
    private List<String> subjects;

    public FacultyDto() {}

    public static FacultyDto from(User u) {
        if (u == null) return null;
        FacultyDto d = new FacultyDto();
        d.id = u.getId();
        d.name = u.getName();
        d.email = u.getEmail();
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

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getOffice() { return office; }
    public void setOffice(String office) { this.office = office; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
}
