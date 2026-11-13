package com.srmap.booking.dto;

import com.srmap.booking.entity.Room;
import java.util.Arrays;
import java.util.List;

public class RoomDto {
    private Long id;
    private String name;
    private String building;
    private String block;
    private Integer floor;
    private String type;
    private int capacity;
    private List<String> features;
    private boolean active;

    public RoomDto() {}

    public static RoomDto from(Room r) {
        if (r == null) return null;
        RoomDto d = new RoomDto();
        d.id = r.getId();
        d.name = r.getName();
        d.building = r.getBuilding();
        d.block = r.getBlock();
        d.floor = r.getFloor();
        d.type = r.getType() != null ? r.getType().name() : null;
        d.capacity = r.getCapacity();
        String raw = r.getFeatures();
        d.features = (raw == null || raw.isBlank())
            ? List.of()
            : Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
        d.active = r.isActive();
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
