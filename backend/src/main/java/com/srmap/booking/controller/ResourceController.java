package com.srmap.booking.controller;

import com.srmap.booking.dto.AvailabilityResponse;
import com.srmap.booking.dto.ResourceDto;
import com.srmap.booking.service.AvailabilityService;
import com.srmap.booking.service.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final AvailabilityService availabilityService;

    public ResourceController(ResourceService resourceService, AvailabilityService availabilityService) {
        this.resourceService = resourceService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public ResponseEntity<List<ResourceDto>> getResources() {
        return ResponseEntity.ok(resourceService.getActiveResources());
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<AvailabilityResponse> getResourceAvailability(
            @PathVariable Long id,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(availabilityService.getResourceAvailability(id, date));
    }
}
