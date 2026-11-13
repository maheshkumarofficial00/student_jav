package com.srmap.booking.dto;

import com.srmap.booking.entity.Resource;

public class ResourceDto {
    private Long id;
    private String name;
    private String category;
    private String description;
    private boolean active;

    public ResourceDto() {}

    public static ResourceDto from(Resource r) {
        if (r == null) return null;
        ResourceDto d = new ResourceDto();
        d.id = r.getId();
        d.name = r.getName();
        d.category = r.getCategory();
        d.description = r.getDescription();
        d.active = r.isActive();
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
