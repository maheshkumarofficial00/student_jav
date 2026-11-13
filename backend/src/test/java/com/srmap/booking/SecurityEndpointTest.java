package com.srmap.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email,
                "password", password
        ));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    void testPublicFacultyEndpointWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/faculty"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0].email", notNullValue()));
    }

    @Test
    void testPublicRoomsEndpointWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void testPublicResourcesEndpointWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void testPublicFacultyAvailabilityWithoutAuth() throws Exception {
        String today = LocalDate.now().toString();
        mockMvc.perform(get("/api/faculty/2/availability").param("date", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date", is(today)))
                .andExpect(jsonPath("$.free", notNullValue()));
    }

    @Test
    void testPublicCombinedAvailabilityWithoutAuth() throws Exception {
        String today = LocalDate.now().toString();
        String nextWeek = LocalDate.now().plusDays(5).toString();
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "facultyId", 2,
                "needRoom", false,
                "dateFrom", today,
                "dateTo", nextWeek,
                "durationMinutes", 30
        ));

        mockMvc.perform(post("/api/availability/combined")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes", is(30)))
                .andExpect(jsonPath("$.windows", notNullValue()));
    }

    @Test
    void testAuthMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Please sign in again.")));
    }

    @Test
    void testLoginAndAuthMeFlow() throws Exception {
        String token = login("student1@srmap.edu.in", "Campus@123");
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("student1@srmap.edu.in")))
                .andExpect(jsonPath("$.role", is("STUDENT")));
    }

    @Test
    void testAdminStatsAccessControl() throws Exception {
        // 1. Unauthenticated -> 401
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Please sign in again.")));

        // 2. Student token -> 403
        String studentToken = login("student1@srmap.edu.in", "Campus@123");
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You do not have access to this.")));

        // 3. Faculty token -> 403
        String facultyToken = login("faculty1@srmap.edu.in", "Campus@123");
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You do not have access to this.")));

        // 4. Admin token -> 200
        String adminToken = login("admin@srmap.edu.in", "Campus@123");
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facultyCount", greaterThanOrEqualTo(1)));
    }

    @Test
    void testAppointmentsMineAccessControl() throws Exception {
        // Unauthenticated -> 401
        mockMvc.perform(get("/api/appointments/mine"))
                .andExpect(status().isUnauthorized());

        // Student -> 200
        String studentToken = login("student1@srmap.edu.in", "Campus@123");
        mockMvc.perform(get("/api/appointments/mine")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));

        // Faculty -> 403
        String facultyToken = login("faculty1@srmap.edu.in", "Campus@123");
        mockMvc.perform(get("/api/appointments/mine")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAppointmentsIncomingAccessControl() throws Exception {
        // Unauthenticated -> 401
        mockMvc.perform(get("/api/appointments/incoming"))
                .andExpect(status().isUnauthorized());

        // Student -> 403
        String studentToken = login("student1@srmap.edu.in", "Campus@123");
        mockMvc.perform(get("/api/appointments/incoming")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Faculty -> 200
        String facultyToken = login("faculty1@srmap.edu.in", "Campus@123");
        mockMvc.perform(get("/api/appointments/incoming")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    void testFacultyScheduleProtectedFromUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/faculty/2/schedule"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Please sign in again.")));
    }

    @Test
    void testRegisterNewStudent() throws Exception {
        String uniqueEmail = "newstudent_" + System.currentTimeMillis() + "@srmap.edu.in";
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "Test New Student",
                "email", uniqueEmail,
                "password", "Student@2024"
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is(uniqueEmail)))
                .andExpect(jsonPath("$.user.role", is("STUDENT")));
    }

    @Test
    void testAdminRoomAndResourceCrudViaApi() throws Exception {
        String adminToken = login("admin@srmap.edu.in", "Campus@123");

        // 1. Create Room as Admin
        String roomBody = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "Executive Boardroom",
                "building", "Admin Block",
                "type", "MEETING_ROOM",
                "capacity", 20,
                "features", java.util.List.of("projector", "smartboard"),
                "active", true
        ));

        MvcResult roomRes = mockMvc.perform(post("/api/admin/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Executive Boardroom")))
                .andReturn();

        JsonNode roomJson = objectMapper.readTree(roomRes.getResponse().getContentAsString());
        long roomId = roomJson.get("id").asLong();

        // 2. Update Room as Admin
        String updateRoomBody = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "Renamed Boardroom",
                "building", "Admin Block, Floor 2",
                "type", "MEETING_ROOM",
                "capacity", 22,
                "active", true
        ));

        mockMvc.perform(put("/api/admin/rooms/" + roomId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRoomBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Renamed Boardroom")))
                .andExpect(jsonPath("$.capacity", is(22)));

        // 3. Create Resource as Admin
        String resBody = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "VR Headset Kit",
                "category", "AR/VR",
                "description", "Meta Quest 3 development kit",
                "active", true
        ));

        mockMvc.perform(post("/api/admin/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("VR Headset Kit")));
    }

    @Test
    void testStudentForbiddenFromAdminCrud() throws Exception {
        String studentToken = login("student1@srmap.edu.in", "Campus@123");

        String roomBody = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "Hacked Room",
                "type", "CLASSROOM",
                "capacity", 50
        ));

        mockMvc.perform(post("/api/admin/rooms")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You do not have access to this.")));
    }

    @Test
    void testStudentAppointmentAndBookingCreationViaApi() throws Exception {
        String studentToken = login("student1@srmap.edu.in", "Campus@123");

        // Find future weekday
        LocalDate futureDate = LocalDate.now().plusDays(8);
        while (futureDate.getDayOfWeek().getValue() >= 6) {
            futureDate = futureDate.plusDays(1);
        }

        // Create appointment with faculty2
        String apptBody = objectMapper.writeValueAsString(java.util.Map.of(
                "facultyId", 2,
                "date", futureDate.toString(),
                "start", "14:00",
                "end", "15:00",
                "purpose", "Project consultation"
        ));

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(apptBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.purpose", is("Project consultation")));

        // Create booking for a room
        String bookingBody = objectMapper.writeValueAsString(java.util.Map.of(
                "roomId", 2,
                "date", futureDate.toString(),
                "start", "16:00",
                "end", "17:00",
                "purpose", "Club meeting"
        ));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.purpose", is("Club meeting")));
    }
}
