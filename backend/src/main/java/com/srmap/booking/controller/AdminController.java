package com.srmap.booking.controller;

import com.srmap.booking.dto.*;
import com.srmap.booking.entity.User;
import com.srmap.booking.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AppointmentService appointmentService;
    private final BookingService bookingService;
    private final RoomService roomService;
    private final ResourceService resourceService;
    private final AuthService authService;

    public AdminController(
            AdminService adminService,
            AppointmentService appointmentService,
            BookingService bookingService,
            RoomService roomService,
            ResourceService resourceService,
            AuthService authService) {
        this.adminService = adminService;
        this.appointmentService = appointmentService;
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.resourceService = resourceService;
        this.authService = authService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentDto>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingDto>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<AdminUserDto> createUser(@RequestBody AdminUserRequest req) {
        return ResponseEntity.ok(adminService.createUser(req));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserDto> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserRequest req,
            Authentication authentication) {
        User currentUser = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(adminService.updateUser(id, req, currentUser));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<RoomDto>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @PostMapping("/rooms")
    public ResponseEntity<RoomDto> createRoom(@RequestBody RoomRequest req) {
        return ResponseEntity.ok(roomService.createRoom(req));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<RoomDto> updateRoom(@PathVariable Long id, @RequestBody RoomRequest req) {
        return ResponseEntity.ok(roomService.updateRoom(id, req));
    }

    @GetMapping("/resources")
    public ResponseEntity<List<ResourceDto>> getAllResources() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }

    @PostMapping("/resources")
    public ResponseEntity<ResourceDto> createResource(@RequestBody ResourceRequest req) {
        return ResponseEntity.ok(resourceService.createResource(req));
    }

    @PutMapping("/resources/{id}")
    public ResponseEntity<ResourceDto> updateResource(@PathVariable Long id, @RequestBody ResourceRequest req) {
        return ResponseEntity.ok(resourceService.updateResource(id, req));
    }
}
