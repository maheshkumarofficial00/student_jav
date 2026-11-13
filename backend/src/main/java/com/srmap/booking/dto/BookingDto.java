package com.srmap.booking.dto;

import com.srmap.booking.entity.Booking;
import java.util.List;

public class BookingDto {
    private Long id;
    private Long userId;
    private Long roomId;
    private List<Long> resourceIds;
    private String date;
    private String start;
    private String end;
    private String purpose;
    private String status;
    private String userName;
    private String roomName;
    private String roomLocation;
    private List<String> resourceNames;

    public BookingDto() {}

    public static BookingDto from(Booking b) {
        if (b == null) return null;
        BookingDto d = new BookingDto();
        d.id = b.getId();
        d.userId = b.getUser() != null ? b.getUser().getId() : null;
        d.roomId = b.getRoom() != null ? b.getRoom().getId() : null;
        List<com.srmap.booking.entity.Resource> resList = b.getResources() != null ? b.getResources() : List.of();
        d.resourceIds = resList.stream().map(com.srmap.booking.entity.Resource::getId).toList();
        d.date = b.getDate() != null ? b.getDate().toString() : null;
        d.start = b.getStartTime();
        d.end = b.getEndTime();
        d.purpose = b.getPurpose();
        d.status = b.getStatus() != null ? b.getStatus().name() : null;
        d.userName = b.getUser() != null ? b.getUser().getName() : null;
        d.roomName = b.getRoom() != null ? b.getRoom().getName() : null;
        d.roomLocation = b.getRoom() != null ? b.getRoom().getBuilding() : null;
        d.resourceNames = resList.stream().map(com.srmap.booking.entity.Resource::getName).toList();
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public List<Long> getResourceIds() { return resourceIds; }
    public void setResourceIds(List<Long> resourceIds) { this.resourceIds = resourceIds; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }

    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomLocation() { return roomLocation; }
    public void setRoomLocation(String roomLocation) { this.roomLocation = roomLocation; }

    public List<String> getResourceNames() { return resourceNames; }
    public void setResourceNames(List<String> resourceNames) { this.resourceNames = resourceNames; }
}
