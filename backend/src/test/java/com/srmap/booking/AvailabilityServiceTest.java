package com.srmap.booking;

import com.srmap.booking.dto.AvailabilityResponse;
import com.srmap.booking.dto.CombinedAvailabilityRequest;
import com.srmap.booking.dto.CombinedAvailabilityResponse;
import com.srmap.booking.entity.Room;
import com.srmap.booking.entity.User;
import com.srmap.booking.repository.RoomRepository;
import com.srmap.booking.repository.UserRepository;
import com.srmap.booking.service.AvailabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AvailabilityServiceTest {

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void testFacultyAvailability() {
        User faculty = userRepository.findByEmailIgnoreCase("faculty1@srmap.edu.in").orElseThrow();
        LocalDate futureDate = LocalDate.now().plusDays(5);

        AvailabilityResponse res = availabilityService.getFacultyAvailability(faculty.getId(), futureDate.toString());
        assertNotNull(res);
        assertEquals(futureDate.toString(), res.getDate());
        assertNotNull(res.getWindows());
        assertNotNull(res.getFree());
    }

    @Test
    void testRoomAvailability() {
        Room room = roomRepository.findAll().get(0);
        LocalDate futureDate = LocalDate.now().plusDays(3);

        AvailabilityResponse res = availabilityService.getRoomAvailability(room.getId(), futureDate.toString());
        assertNotNull(res);
        assertEquals(futureDate.toString(), res.getDate());
    }

    @Test
    void testCombinedAvailability() {
        User faculty = userRepository.findByEmailIgnoreCase("faculty1@srmap.edu.in").orElseThrow();
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = LocalDate.now().plusDays(4);

        CombinedAvailabilityRequest req = new CombinedAvailabilityRequest();
        req.setFacultyId(faculty.getId());
        req.setNeedRoom(true);
        req.setDateFrom(from.toString());
        req.setDateTo(to.toString());
        req.setDurationMinutes(30);

        CombinedAvailabilityResponse res = availabilityService.getCombinedAvailability(req);
        assertNotNull(res);
        assertEquals(30, res.getDurationMinutes());
        assertNotNull(res.getWindows());
    }
}
