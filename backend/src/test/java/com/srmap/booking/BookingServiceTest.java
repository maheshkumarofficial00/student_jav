package com.srmap.booking;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.BookingDto;
import com.srmap.booking.dto.BookingRequest;
import com.srmap.booking.entity.Resource;
import com.srmap.booking.entity.Room;
import com.srmap.booking.entity.User;
import com.srmap.booking.repository.ResourceRepository;
import com.srmap.booking.repository.RoomRepository;
import com.srmap.booking.repository.UserRepository;
import com.srmap.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Test
    void testGetMyBookings() {
        User faculty = userRepository.findByEmailIgnoreCase("faculty2@srmap.edu.in").orElseThrow();
        List<BookingDto> mine = bookingService.getMyBookings(faculty);
        assertNotNull(mine);
        assertFalse(mine.isEmpty());
    }

    @Test
    void testCreateRoomBookingSuccess() {
        User student = userRepository.findByEmailIgnoreCase("student1@srmap.edu.in").orElseThrow();
        Room room = roomRepository.findAll().stream().filter(Room::isActive).findFirst().orElseThrow();

        // Future weekday
        LocalDate testDate = LocalDate.now().plusDays(6);
        while (testDate.getDayOfWeek().getValue() == 7) {
            testDate = testDate.plusDays(1);
        }

        BookingRequest req = new BookingRequest();
        req.setRoomId(room.getId());
        req.setDate(testDate.toString());
        req.setStart("09:00");
        req.setEnd("10:30");
        req.setPurpose("Study group session");

        BookingDto created = bookingService.createBooking(req, student);
        assertNotNull(created.getId());
        assertEquals("CONFIRMED", created.getStatus());
        assertEquals("Study group session", created.getPurpose());
        assertEquals(room.getId(), created.getRoomId());
        assertEquals(room.getName(), created.getRoomName());
    }

    @Test
    void testCreateResourceBookingSuccess() {
        User faculty = userRepository.findByEmailIgnoreCase("faculty1@srmap.edu.in").orElseThrow();
        Resource resource = resourceRepository.findAll().stream().filter(Resource::isActive).findFirst().orElseThrow();

        LocalDate testDate = LocalDate.now().plusDays(6);
        while (testDate.getDayOfWeek().getValue() == 7) {
            testDate = testDate.plusDays(1);
        }

        BookingRequest req = new BookingRequest();
        req.setResourceIds(List.of(resource.getId()));
        req.setDate(testDate.toString());
        req.setStart("14:00");
        req.setEnd("16:00");
        req.setPurpose("Hardware demonstration lab");

        BookingDto created = bookingService.createBooking(req, faculty);
        assertNotNull(created.getId());
        assertEquals("CONFIRMED", created.getStatus());
        assertTrue(created.getResourceIds().contains(resource.getId()));
    }

    @Test
    void testCreateBookingValidation() {
        User student = userRepository.findByEmailIgnoreCase("student1@srmap.edu.in").orElseThrow();

        // Neither room nor resource provided
        BookingRequest emptyReq = new BookingRequest();
        emptyReq.setDate(LocalDate.now().plusDays(5).toString());
        emptyReq.setStart("10:00");
        emptyReq.setEnd("11:00");
        emptyReq.setPurpose("Valid purpose");

        ApiException ex = assertThrows(ApiException.class, () -> bookingService.createBooking(emptyReq, student));
        assertEquals("Choose a room or at least one resource.", ex.getMessage());

        // Purpose too short
        Room room = roomRepository.findAll().get(0);
        BookingRequest shortPurpose = new BookingRequest();
        shortPurpose.setRoomId(room.getId());
        shortPurpose.setDate(LocalDate.now().plusDays(5).toString());
        shortPurpose.setStart("10:00");
        shortPurpose.setEnd("11:00");
        shortPurpose.setPurpose("No");

        ApiException exPurpose = assertThrows(ApiException.class, () -> bookingService.createBooking(shortPurpose, student));
        assertEquals("Add a short purpose for the reservation.", exPurpose.getMessage());
    }

    @Test
    void testCancelBooking() {
        User faculty = userRepository.findByEmailIgnoreCase("faculty2@srmap.edu.in").orElseThrow();
        List<BookingDto> mine = bookingService.getMyBookings(faculty);
        BookingDto booking = mine.get(0);

        BookingDto cancelled = bookingService.cancelBooking(booking.getId(), faculty);
        assertEquals("CANCELLED", cancelled.getStatus());
    }

    @Test
    void testAdminCanCancelAnyBooking() {
        User admin = userRepository.findByEmailIgnoreCase("admin@srmap.edu.in").orElseThrow();
        List<BookingDto> all = bookingService.getAllBookings();
        assertFalse(all.isEmpty());

        BookingDto booking = all.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .findFirst()
                .orElseThrow();

        BookingDto cancelled = bookingService.cancelBooking(booking.getId(), admin);
        assertEquals("CANCELLED", cancelled.getStatus());
    }
}
