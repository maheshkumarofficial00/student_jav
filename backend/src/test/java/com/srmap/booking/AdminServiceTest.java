package com.srmap.booking;

import com.srmap.booking.dto.AdminStatsDto;
import com.srmap.booking.dto.AdminUserDto;
import com.srmap.booking.dto.AdminUserRequest;
import com.srmap.booking.entity.User;
import com.srmap.booking.repository.UserRepository;
import com.srmap.booking.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testGetStats() {
        AdminStatsDto stats = adminService.getStats();
        assertNotNull(stats);
        assertTrue(stats.getActiveUsers() > 0);
        assertTrue(stats.getFacultyCount() > 0);
        assertTrue(stats.getStudentCount() > 0);
        assertTrue(stats.getRooms() > 0);
        assertTrue(stats.getResources() > 0);
    }

    @Test
    void testGetAllUsers() {
        List<AdminUserDto> users = adminService.getAllUsers();
        assertNotNull(users);
        assertFalse(users.isEmpty());
    }

    @Test
    void testCreateAndUpdateUser() {
        User admin = userRepository.findByEmailIgnoreCase("admin@srmap.edu.in").orElseThrow();

        AdminUserRequest createReq = new AdminUserRequest();
        createReq.setName("New Faculty");
        createReq.setEmail("new.faculty@srmap.edu.in");
        createReq.setPassword("Campus@123");
        createReq.setRole("FACULTY");
        createReq.setDepartment("Computer Science");
        createReq.setOffice("F-101");

        AdminUserDto created = adminService.createUser(createReq);
        assertNotNull(created.getId());
        assertEquals("new.faculty@srmap.edu.in", created.getEmail());
        assertEquals("FACULTY", created.getRole());

        // Update user
        AdminUserRequest updateReq = new AdminUserRequest();
        updateReq.setName("Updated Faculty Name");
        updateReq.setEmail("new.faculty@srmap.edu.in");
        updateReq.setRole("FACULTY");
        updateReq.setDepartment("Data Science");

        AdminUserDto updated = adminService.updateUser(created.getId(), updateReq, admin);
        assertEquals("Updated Faculty Name", updated.getName());
        assertEquals("Data Science", updated.getDepartment());
    }
}
