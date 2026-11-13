package com.srmap.booking.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    /** User-visible building description, e.g. "SR Block, Floor 3" */
    @Column(length = 150)
    private String building;

    /** Campus block name, e.g. "SR Block". Null for non-campus rooms. */
    @Column(length = 80)
    private String block;

    /** Floor number within block. Null for non-campus rooms. */
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType type;

    @Column(nullable = false)
    private int capacity;

    /** Comma-separated list of features, e.g. "projector,whiteboard" */
    @Column(length = 300)
    private String features;

    @Column(nullable = false)
    private boolean active = true;

    public enum RoomType {
        CLASSROOM, MEETING_ROOM, SEMINAR_HALL
    }

    public Room() {
    }

    public Room(Long id, String name, String building, String block, Integer floor,
                RoomType type, int capacity, String features, boolean active) {
        this.id = id;
        this.name = name;
        this.building = building;
        this.block = block;
        this.floor = floor;
        this.type = type;
        this.capacity = capacity;
        this.features = features;
        this.active = active;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String building;
        private String block;
        private Integer floor;
        private RoomType type;
        private int capacity;
        private String features;
        private boolean active = true;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder building(String building) { this.building = building; return this; }
        public Builder block(String block) { this.block = block; return this; }
        public Builder floor(Integer floor) { this.floor = floor; return this; }
        public Builder type(RoomType type) { this.type = type; return this; }
        public Builder capacity(int capacity) { this.capacity = capacity; return this; }
        public Builder features(String features) { this.features = features; return this; }
        public Builder active(boolean active) { this.active = active; return this; }

        public Room build() {
            return new Room(id, name, building, block, floor, type, capacity, features, active);
        }
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

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
