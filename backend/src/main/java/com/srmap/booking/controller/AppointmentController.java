package com.srmap.booking.controller;

import com.srmap.booking.dto.AppointmentDto;
import com.srmap.booking.dto.AppointmentRequest;
import com.srmap.booking.dto.StatusRequest;
import com.srmap.booking.entity.User;
import com.srmap.booking.service.AppointmentService;
import com.srmap.booking.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthService authService;

    public AppointmentController(AppointmentService appointmentService, AuthService authService) {
        this.appointmentService = appointmentService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<AppointmentDto> createAppointment(
            @RequestBody AppointmentRequest req,
            Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(appointmentService.createAppointment(req, user));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<AppointmentDto>> getMyAppointments(Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(appointmentService.getMyAppointments(user));
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<AppointmentDto>> getIncomingAppointments(Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(appointmentService.getIncomingAppointments(user));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentDto> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest req,
            Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(appointmentService.updateStatus(id, req, user));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentDto> cancelAppointment(
            @PathVariable Long id,
            Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, user));
    }
}
