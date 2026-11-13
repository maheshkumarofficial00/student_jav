package com.srmap.booking.dto;

/** Request body for admin resource create/update. */
public class ResourceRequest {
    private String name;
    private String category;
    private String description;
    private Boolean active;

    public ResourceRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
