package com.srmap.booking.controller;

import com.srmap.booking.dto.AvailabilityResponse;
import com.srmap.booking.dto.RoomDto;
import com.srmap.booking.service.AvailabilityService;
import com.srmap.booking.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final AvailabilityService availabilityService;

    public RoomController(RoomService roomService, AvailabilityService availabilityService) {
        this.roomService = roomService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public ResponseEntity<List<RoomDto>> getRooms(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(roomService.getActiveRooms(type));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<AvailabilityResponse> getRoomAvailability(
            @PathVariable Long id,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(availabilityService.getRoomAvailability(id, date));
    }
}
