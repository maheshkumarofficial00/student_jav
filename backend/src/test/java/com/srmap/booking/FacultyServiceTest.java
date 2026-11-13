package com.srmap.booking;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.ScheduleBlockRequest;
import com.srmap.booking.dto.ScheduleResponse;
import com.srmap.booking.dto.WorkingHoursRequest;
import com.srmap.booking.entity.User;
import com.srmap.booking.repository.UserRepository;
import com.srmap.booking.service.FacultyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FacultyServiceTest {

    @Autowired
    private FacultyService facultyService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testGetSchedule() {
        User faculty = userRepository.findByEmailIgnoreCase("faculty1@srmap.edu.in").orElseThrow();
        ScheduleResponse schedule = facultyService.getSchedule(faculty.getId(), faculty);

        assertNotNull(schedule);
        assertNotNull(schedule.getWorkingHours());
        assertFalse(schedule.getWorkingHours().isEmpty());
    }

    @Test
    void testUpdateWorkingHours() {
        User faculty = userRepository.findByEmailIgnoreCase("faculty1@srmap.edu.in").orElseThrow();

        WorkingHoursRequest.TimeRangeDto slot = new WorkingHoursRequest.TimeRangeDto();
        slot.setStart("09:00");
        slot.setEnd("16:00");

        WorkingHoursRequest req = new WorkingHoursRequest();
        req.setWorkingHours(Map.of("1", List.of(slot)));

        Map<String, Object> result = facultyService.updateWorkingHours(faculty.getId(), req, faculty);
        assertNotNull(result);
        assertTrue(result.containsKey("workingHours"));
    }

    @Test
    void testAddAndDeleteBlock() {
        User faculty = userRepository.findByEmailIgnoreCase("faculty1@srmap.edu.in").orElseThrow();
        LocalDate futureDate = LocalDate.now().plusDays(10);

        ScheduleBlockRequest blockReq = new ScheduleBlockRequest();
        blockReq.setDate(futureDate.toString());
        blockReq.setStart("13:00");
        blockReq.setEnd("15:00");
        blockReq.setReason("Doctor Appointment");

        ScheduleResponse.ScheduleBlockDto block = facultyService.addBlock(faculty.getId(), blockReq, faculty);
        assertNotNull(block);
        assertEquals(futureDate.toString(), block.getDate());
        assertEquals("Doctor Appointment", block.getReason());

        // Delete block
        Map<String, Object> delRes = facultyService.deleteBlock(faculty.getId(), block.getId(), faculty);
        assertNotNull(delRes);
    }

    @Test
    void testForbiddenScheduleAccess() {
        User faculty = userRepository.findByEmailIgnoreCase("faculty1@srmap.edu.in").orElseThrow();
        User student = userRepository.findByEmailIgnoreCase("student1@srmap.edu.in").orElseThrow();

        ApiException ex = assertThrows(ApiException.class, () -> facultyService.getSchedule(faculty.getId(), student));
        assertEquals("You can only manage your own schedule.", ex.getMessage());
    }
}
