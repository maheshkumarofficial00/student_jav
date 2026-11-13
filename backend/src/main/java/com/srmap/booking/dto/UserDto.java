package com.srmap.booking.dto;

import com.srmap.booking.entity.User;

/** Public user representation (never includes password). */
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String role;

    public UserDto() {}

    public UserDto(Long id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public static UserDto from(User u) {
        if (u == null) return null;
        UserDto d = new UserDto();
        d.id = u.getId();
        d.name = u.getName();
        d.email = u.getEmail();
        d.role = u.getRole() != null ? u.getRole().name() : null;
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
}
