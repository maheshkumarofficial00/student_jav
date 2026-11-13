package com.srmap.booking.service;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.AdminStatsDto;
import com.srmap.booking.dto.AdminUserDto;
import com.srmap.booking.dto.AdminUserRequest;
import com.srmap.booking.entity.Appointment;
import com.srmap.booking.entity.User;
import com.srmap.booking.entity.WorkingHour;
import com.srmap.booking.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ResourceRepository resourceRepository;
    private final AppointmentRepository appointmentRepository;
    private final BookingRepository bookingRepository;
    private final WorkingHourRepository workingHourRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            RoomRepository roomRepository,
            ResourceRepository resourceRepository,
            AppointmentRepository appointmentRepository,
            BookingRepository bookingRepository,
            WorkingHourRepository workingHourRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.resourceRepository = resourceRepository;
        this.appointmentRepository = appointmentRepository;
        this.bookingRepository = bookingRepository;
        this.workingHourRepository = workingHourRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        LocalDate today = LocalDate.now();
        List<User> allUsers = userRepository.findAll();
        long activeUsers = allUsers.stream().filter(User::isActive).count();
        long facultyCount = allUsers.stream().filter(u -> u.isActive() && u.getRole() == User.Role.FACULTY).count();
        long studentCount = allUsers.stream().filter(u -> u.isActive() && u.getRole() == User.Role.STUDENT).count();
        long rooms = roomRepository.findByActiveTrue().size();
        long resources = resourceRepository.findByActiveTrue().size();
        long pendingAppts = appointmentRepository.countByStatus(Appointment.Status.PENDING);
        long upcomingAppts = appointmentRepository.countUpcoming(today);
        long upcomingRes = bookingRepository.countUpcomingConfirmed(today);

        return new AdminStatsDto(
                activeUsers,
                facultyCount,
                studentCount,
                rooms,
                resources,
                pendingAppts,
                upcomingAppts,
                upcomingRes
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> getAllUsers() {
        return userRepository.findAll().stream().map(AdminUserDto::from).toList();
    }

    @Transactional
    public AdminUserDto createUser(AdminUserRequest req) {
        String name = req.getName() != null ? req.getName().trim() : "";
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        String password = req.getPassword() != null ? req.getPassword() : "";
        String roleStr = req.getRole() != null ? req.getRole().trim().toUpperCase() : "";

        if (name.length() < 2 || (!"STUDENT".equals(roleStr) && !"FACULTY".equals(roleStr) && !"ADMIN".equals(roleStr))) {
            throw ApiException.badRequest("Name and a valid role are required.");
        }

        AuthService.validateEmail(email);
        AuthService.validatePassword(password);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("A user with this email already exists.");
        }

        User.Role role = User.Role.valueOf(roleStr);
        User user = User.builder()
                .name(name)
                .email(email)
                .role(role)
                .active(req.getActive() == null || req.getActive())
                .password(passwordEncoder.encode(password))
                .build();

        if (role == User.Role.FACULTY) {
            user.setDepartment(req.getDepartment() != null ? req.getDepartment().trim() : "");
            user.setOffice(req.getOffice() != null ? req.getOffice().trim() : "");
            user.setTitle(req.getTitle() != null ? req.getTitle().trim() : "Faculty");
            if (req.getSubjects() != null) {
                user.setSubjects(String.join(",", req.getSubjects()));
            }
        }

        user = userRepository.save(user);

        if (role == User.Role.FACULTY) {
            List<WorkingHour> defaultHours = new ArrayList<>();
            for (int d = 1; d <= 5; d++) {
                defaultHours.add(WorkingHour.builder()
                        .faculty(user)
                        .dayOfWeek(d)
                        .startTime("09:00")
                        .endTime("17:00")
                        .build());
            }
            workingHourRepository.saveAll(defaultHours);
        }

        return AdminUserDto.from(user);
    }

    @Transactional
    public AdminUserDto updateUser(Long id, AdminUserRequest req, User currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found."));

        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : user.getEmail();

        if (userRepository.findByEmailIgnoreCase(email).filter(u -> !u.getId().equals(id)).isPresent()) {
            throw ApiException.conflict("A user with this email already exists.");
        }

        if (user.getId().equals(currentUser.getId())) {
            if ((req.getActive() != null && !req.getActive()) ||
                    (req.getRole() != null && !user.getRole().name().equalsIgnoreCase(req.getRole()))) {
                throw ApiException.conflict("You cannot deactivate or change the role of your own account.");
            }
        }

        if (req.getName() != null && !req.getName().isBlank()) {
            user.setName(req.getName().trim());
        }
        user.setEmail(email);

        if (req.getRole() != null && !req.getRole().isBlank()) {
            try {
                user.setRole(User.Role.valueOf(req.getRole().trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        if (req.getActive() != null) {
            user.setActive(req.getActive());
        }

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            if (req.getPassword().length() < 8) {
                throw ApiException.badRequest("Password must be at least 8 characters.");
            }
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        if (user.getRole() == User.Role.FACULTY) {
            if (req.getDepartment() != null) user.setDepartment(req.getDepartment().trim());
            if (req.getOffice() != null) user.setOffice(req.getOffice().trim());
            if (req.getTitle() != null) user.setTitle(req.getTitle().trim());
            if (req.getSubjects() != null) {
                user.setSubjects(String.join(",", req.getSubjects()));
            }
        }

        user = userRepository.save(user);
        return AdminUserDto.from(user);
    }
}
