package com.srmap.booking.controller;

import com.srmap.booking.dto.CombinedAvailabilityRequest;
import com.srmap.booking.dto.CombinedAvailabilityResponse;
import com.srmap.booking.service.AvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/combined")
    public ResponseEntity<CombinedAvailabilityResponse> getCombinedAvailability(@RequestBody CombinedAvailabilityRequest req) {
        return ResponseEntity.ok(availabilityService.getCombinedAvailability(req));
    }
}
