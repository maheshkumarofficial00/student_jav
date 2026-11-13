package com.srmap.booking.dto;

/** Request body for POST /api/availability/combined */
public class CombinedAvailabilityRequest {
    private Long facultyId;
    private Boolean needRoom;
    private Long roomId;
    private String roomType;
    private String block;
    private Integer floor;
    private Integer minCapacity;
    private String dateFrom;
    private String dateTo;
    private Integer durationMinutes;

    public CombinedAvailabilityRequest() {}

    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }

    public Boolean getNeedRoom() { return needRoom; }
    public void setNeedRoom(Boolean needRoom) { this.needRoom = needRoom; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public Integer getMinCapacity() { return minCapacity; }
    public void setMinCapacity(Integer minCapacity) { this.minCapacity = minCapacity; }

    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }

    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
}
