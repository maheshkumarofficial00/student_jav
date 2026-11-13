package com.srmap.booking.dto;

import java.util.List;

/** Request body for admin room create/update. */
public class RoomRequest {
    private String name;
    private String building;
    private String block;
    private Integer floor;
    private String type;
    private Integer capacity;
    private List<String> features;
    private Boolean active;

    public RoomRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
