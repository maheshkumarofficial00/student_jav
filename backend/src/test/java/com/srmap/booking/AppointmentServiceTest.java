package com.srmap.booking;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.AppointmentDto;
import com.srmap.booking.dto.AppointmentRequest;
import com.srmap.booking.dto.StatusRequest;
import com.srmap.booking.entity.User;
import com.srmap.booking.repository.UserRepository;
import com.srmap.booking.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AppointmentServiceTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testGetAppointments() {
        User student = userRepository.findByEmailIgnoreCase("student1@srmap.edu.in").orElseThrow();
        List<AppointmentDto> myAppts = appointmentService.getMyAppointments(student);
        assertNotNull(myAppts);
        assertFalse(myAppts.isEmpty());

        User faculty = userRepository.findByEmailIgnoreCase("faculty1@srmap.edu.in").orElseThrow();
        List<AppointmentDto> incoming = appointmentService.getIncomingAppointments(faculty);
        assertNotNull(incoming);
        assertFalse(incoming.isEmpty());
    }

    @Test
    void testCreateAppointmentSuccess() {
        User student = userRepository.findByEmailIgnoreCase("student1@srmap.edu.in").orElseThrow();
        User faculty = userRepository.findByEmailIgnoreCase("faculty5@srmap.edu.in").orElseThrow();

        // Find a future weekday for testing
        LocalDate testDate = LocalDate.now().plusDays(7);
        while (testDate.getDayOfWeek().getValue() >= 6) {
            testDate = testDate.plusDays(1);
        }

        AppointmentRequest req = new AppointmentRequest();
        req.setFacultyId(faculty.getId());
        req.setDate(testDate.toString());
        req.setStart("10:00");
        req.setEnd("11:00");
        req.setPurpose("Discuss research paper topic");

        AppointmentDto created = appointmentService.createAppointment(req, student);
        assertNotNull(created.getId());
        assertEquals("PENDING", created.getStatus());
        assertEquals("Discuss research paper topic", created.getPurpose());
        assertEquals(student.getName(), created.getStudentName());
        assertEquals(faculty.getName(), created.getFacultyName());
    }

    @Test
    void testCreateAppointmentValidation() {
        User student = userRepository.findByEmailIgnoreCase("student1@srmap.edu.in").orElseThrow();
        User faculty = userRepository.findByEmailIgnoreCase("faculty1@srmap.edu.in").orElseThrow();

        // Purpose too short (< 3 characters)
        AppointmentRequest shortPurposeReq = new AppointmentRequest();
        shortPurposeReq.setFacultyId(faculty.getId());
        shortPurposeReq.setDate(LocalDate.now().plusDays(3).toString());
        shortPurposeReq.setStart("10:00");
        shortPurposeReq.setEnd("11:00");
        shortPurposeReq.setPurpose("Hi");

        ApiException ex = assertThrows(ApiException.class, () ->
                appointmentService.createAppointment(shortPurposeReq, student));
        assertEquals("Add a short purpose for the meeting.", ex.getMessage());

        // Non-student attempting to book appointment
        AppointmentRequest validReq = new AppointmentRequest();
        validReq.setFacultyId(faculty.getId());
        validReq.setDate(LocalDate.now().plusDays(3).toString());
        validReq.setStart("10:00");
        validReq.setEnd("11:00");
        validReq.setPurpose("Faculty cannot request appointments");

        ApiException exRole = assertThrows(ApiException.class, () ->
                appointmentService.createAppointment(validReq, faculty));
        assertEquals("Only students can request appointments.", exRole.getMessage());
    }

    @Test
    void testStatusAndCancelFlow() {
        User faculty = userRepository.findByEmailIgnoreCase("faculty2@srmap.edu.in").orElseThrow();
        User student = userRepository.findByEmailIgnoreCase("student2@srmap.edu.in").orElseThrow();

        List<AppointmentDto> incoming = appointmentService.getIncomingAppointments(faculty);
        AppointmentDto pending = incoming.stream()
                .filter(a -> "PENDING".equals(a.getStatus()))
                .findFirst()
                .orElse(null);

        if (pending != null) {
            StatusRequest statusReq = new StatusRequest();
            statusReq.setStatus("CONFIRMED");
            AppointmentDto confirmed = appointmentService.updateStatus(pending.getId(), statusReq, faculty);
            assertEquals("CONFIRMED", confirmed.getStatus());

            // Student cancels confirmed appointment
            AppointmentDto cancelled = appointmentService.cancelAppointment(confirmed.getId(), student);
            assertEquals("CANCELLED", cancelled.getStatus());
        }
    }

    @Test
    void testAdminCanCancelAnyAppointment() {
        User admin = userRepository.findByEmailIgnoreCase("admin@srmap.edu.in").orElseThrow();
        List<AppointmentDto> all = appointmentService.getAllAppointments();
        assertFalse(all.isEmpty());

        AppointmentDto appt = all.stream()
                .filter(a -> "CONFIRMED".equals(a.getStatus()) || "PENDING".equals(a.getStatus()))
                .findFirst()
                .orElseThrow();

        AppointmentDto cancelled = appointmentService.cancelAppointment(appt.getId(), admin);
        assertEquals("CANCELLED", cancelled.getStatus());
    }
}
