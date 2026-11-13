package com.srmap.booking.controller;

import com.srmap.booking.dto.*;
import com.srmap.booking.entity.User;
import com.srmap.booking.service.AuthService;
import com.srmap.booking.service.AvailabilityService;
import com.srmap.booking.service.FacultyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    private final FacultyService facultyService;
    private final AvailabilityService availabilityService;
    private final AuthService authService;

    public FacultyController(
            FacultyService facultyService,
            AvailabilityService availabilityService,
            AuthService authService) {
        this.facultyService = facultyService;
        this.availabilityService = availabilityService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<FacultyDto>> getFaculty() {
        return ResponseEntity.ok(facultyService.getAllActiveFaculty());
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @PathVariable Long id,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(availabilityService.getFacultyAvailability(id, date));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @PathVariable Long id,
            Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(facultyService.getSchedule(id, user));
    }

    @PutMapping("/{id}/schedule/working-hours")
    public ResponseEntity<Map<String, Object>> updateWorkingHours(
            @PathVariable Long id,
            @RequestBody WorkingHoursRequest req,
            Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(facultyService.updateWorkingHours(id, req, user));
    }

    @PostMapping("/{id}/schedule/blocks")
    public ResponseEntity<ScheduleResponse.ScheduleBlockDto> addBlock(
            @PathVariable Long id,
            @RequestBody ScheduleBlockRequest req,
            Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(facultyService.addBlock(id, req, user));
    }

    @DeleteMapping("/{id}/schedule/blocks/{blockId}")
    public ResponseEntity<Map<String, Object>> deleteBlock(
            @PathVariable Long id,
            @PathVariable Long blockId,
            Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(facultyService.deleteBlock(id, blockId, user));
    }
}
