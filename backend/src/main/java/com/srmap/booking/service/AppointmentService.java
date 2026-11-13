package com.srmap.booking.service;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.AppointmentDto;
import com.srmap.booking.dto.AppointmentRequest;
import com.srmap.booking.dto.StatusRequest;
import com.srmap.booking.entity.Appointment;
import com.srmap.booking.entity.Room;
import com.srmap.booking.entity.User;
import com.srmap.booking.repository.AppointmentRepository;
import com.srmap.booking.repository.RoomRepository;
import com.srmap.booking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final AvailabilityService availabilityService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            RoomRepository roomRepository,
            AvailabilityService availabilityService) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.availabilityService = availabilityService;
    }

    @Transactional
    public AppointmentDto createAppointment(AppointmentRequest req, User student) {
        if (student.getRole() != User.Role.STUDENT) {
            throw ApiException.forbidden("Only students can request appointments.");
        }

        availabilityService.checkTimes(req.getDate(), req.getStart(), req.getEnd());
        LocalDate date = AvailabilityService.parseAndValidateDate(req.getDate());

        String purpose = req.getPurpose() != null ? req.getPurpose().trim() : "";
        if (purpose.length() < 3) {
            throw ApiException.badRequest("Add a short purpose for the meeting.");
        }

        Long facultyId = req.getFacultyId();
        if (facultyId == null) {
            throw ApiException.badRequest("Faculty member is required.");
        }

        User faculty = userRepository.findById(facultyId)
                .filter(u -> u.getRole() == User.Role.FACULTY && u.isActive())
                .orElseThrow(() -> ApiException.notFound("Faculty member not found."));

        if (!availabilityService.isFree("faculty", facultyId, req.getDate(), req.getStart(), req.getEnd())) {
            throw ApiException.conflict("The faculty member is no longer available at that time.");
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

        Appointment appt = Appointment.builder()
                .student(student)
                .faculty(faculty)
                .room(room)
                .date(date)
                .startTime(req.getStart())
                .endTime(req.getEnd())
                .purpose(purpose)
                .status(Appointment.Status.PENDING)
                .build();

        appt = appointmentRepository.save(appt);
        return AppointmentDto.from(appt);
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> getMyAppointments(User student) {
        return appointmentRepository.findByStudentIdOrderByDateAscStartTimeAsc(student.getId()).stream()
                .map(AppointmentDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> getIncomingAppointments(User faculty) {
        return appointmentRepository.findByFacultyIdOrderByDateAscStartTimeAsc(faculty.getId()).stream()
                .map(AppointmentDto::from)
                .toList();
    }

    @Transactional
    public AppointmentDto updateStatus(Long id, StatusRequest req, User faculty) {
        Appointment appt = appointmentRepository.findById(id)
                .filter(a -> a.getFaculty().getId().equals(faculty.getId()))
                .orElseThrow(() -> ApiException.notFound("Appointment not found."));

        String statusStr = req.getStatus() != null ? req.getStatus().trim().toUpperCase() : "";
        if (!"CONFIRMED".equals(statusStr) && !"DECLINED".equals(statusStr)) {
            throw ApiException.badRequest("Status must be CONFIRMED or DECLINED.");
        }

        if (appt.getStatus() != Appointment.Status.PENDING) {
            throw ApiException.conflict("Only pending requests can be answered.");
        }

        appt.setStatus(Appointment.Status.valueOf(statusStr));
        appt = appointmentRepository.save(appt);
        return AppointmentDto.from(appt);
    }

    @Transactional
    public AppointmentDto cancelAppointment(Long id, User currentUser) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Appointment not found."));

        boolean owns = appt.getStudent().getId().equals(currentUser.getId()) ||
                appt.getFaculty().getId().equals(currentUser.getId()) ||
                currentUser.getRole() == User.Role.ADMIN;

        if (!owns) {
            throw ApiException.forbidden("You cannot cancel this appointment.");
        }

        if (appt.getStatus() != Appointment.Status.PENDING && appt.getStatus() != Appointment.Status.CONFIRMED) {
            throw ApiException.conflict("This appointment is already closed.");
        }

        appt.setStatus(Appointment.Status.CANCELLED);
        appt = appointmentRepository.save(appt);
        return AppointmentDto.from(appt);
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> getAllAppointments() {
        return appointmentRepository.findAllByOrderByDateAscStartTimeAsc().stream()
                .map(AppointmentDto::from)
                .toList();
    }
}
