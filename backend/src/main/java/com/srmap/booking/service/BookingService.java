package com.srmap.booking.service;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.BookingDto;
import com.srmap.booking.dto.BookingRequest;
import com.srmap.booking.entity.Booking;
import com.srmap.booking.entity.Resource;
import com.srmap.booking.entity.Room;
import com.srmap.booking.entity.User;
import com.srmap.booking.repository.BookingRepository;
import com.srmap.booking.repository.ResourceRepository;
import com.srmap.booking.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final ResourceRepository resourceRepository;
    private final AvailabilityService availabilityService;

    public BookingService(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            ResourceRepository resourceRepository,
            AvailabilityService availabilityService) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.resourceRepository = resourceRepository;
        this.availabilityService = availabilityService;
    }

    @Transactional
    public BookingDto createBooking(BookingRequest req, User currentUser) {
        availabilityService.checkTimes(req.getDate(), req.getStart(), req.getEnd());
        LocalDate date = AvailabilityService.parseAndValidateDate(req.getDate());

        List<Long> resourceIds = req.getResourceIds() != null ? req.getResourceIds() : List.of();
        if (req.getRoomId() == null && resourceIds.isEmpty()) {
            throw ApiException.badRequest("Choose a room or at least one resource.");
        }

        String purpose = req.getPurpose() != null ? req.getPurpose().trim() : "";
        if (purpose.length() < 3) {
            throw ApiException.badRequest("Add a short purpose for the reservation.");
        }

        Room room = null;
        if (req.getRoomId() != null) {
            room = roomRepository.findById(req.getRoomId())
                    .filter(Room::isActive)
                    .orElseThrow(() -> ApiException.notFound("Room not found."));

            if (!availabilityService.isFree("room", req.getRoomId(), req.getDate(), req.getStart(), req.getEnd())) {
                throw ApiException.conflict("The room is no longer available at that time.");
            }
        }

        List<Resource> resources = new ArrayList<>();
        for (Long resId : resourceIds) {
            Resource res = resourceRepository.findById(resId)
                    .filter(Resource::isActive)
                    .orElseThrow(() -> ApiException.notFound("Resource not found."));

            if (!availabilityService.isFree("resource", resId, req.getDate(), req.getStart(), req.getEnd())) {
                throw ApiException.conflict(res.getName() + " is not available at that time.");
            }
            resources.add(res);
        }

        Booking booking = Booking.builder()
                .user(currentUser)
                .room(room)
                .resources(resources)
                .date(date)
                .startTime(req.getStart())
                .endTime(req.getEnd())
                .purpose(purpose)
                .status(Booking.Status.CONFIRMED)
                .build();

        booking = bookingRepository.save(booking);
        return BookingDto.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getMyBookings(User currentUser) {
        return bookingRepository.findByUserIdOrderByDateAscStartTimeAsc(currentUser.getId()).stream()
                .map(BookingDto::from)
                .toList();
    }

    @Transactional
    public BookingDto cancelBooking(Long id, User currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Reservation not found."));

        boolean owns = booking.getUser().getId().equals(currentUser.getId()) ||
                currentUser.getRole() == User.Role.ADMIN;

        if (!owns) {
            throw ApiException.forbidden("You cannot cancel this reservation.");
        }

        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw ApiException.conflict("This reservation is already cancelled.");
        }

        booking.setStatus(Booking.Status.CANCELLED);
        booking = bookingRepository.save(booking);
        return BookingDto.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getAllBookings() {
        return bookingRepository.findAllByOrderByDateAscStartTimeAsc().stream()
                .map(BookingDto::from)
                .toList();
    }
}
