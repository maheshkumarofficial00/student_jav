package com.srmap.booking.controller;

import com.srmap.booking.dto.BookingDto;
import com.srmap.booking.dto.BookingRequest;
import com.srmap.booking.entity.User;
import com.srmap.booking.service.AuthService;
import com.srmap.booking.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final AuthService authService;

    public BookingController(BookingService bookingService, AuthService authService) {
        this.bookingService = bookingService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(
            @RequestBody BookingRequest req,
            Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(bookingService.createBooking(req, user));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BookingDto>> getMyBookings(Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(bookingService.getMyBookings(user));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingDto> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        User user = authService.getCurrentUserEntity(authentication.getName());
        return ResponseEntity.ok(bookingService.cancelBooking(id, user));
    }
}
