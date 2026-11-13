# Smart Faculty and Resource Booking System - Java Backend

Spring Boot backend replacing the client-side JavaScript mock server (`js/mock.js`), fully preserving the original frontend API contract and business logic.

---

## 1. Technology Stack

- **Java**: 17+
- **Framework**: Spring Boot 3.3.5
- **Build Tool**: Maven (with `mvnw` wrapper included)
- **Database**: MySQL 8.0+ (with Spring Data JPA / Hibernate)
- **Security**: Spring Security 6 with stateless JWT Bearer token authentication & BCrypt password hashing
- **Testing**: JUnit 5, Spring Boot Test, H2 in-memory database for automated tests

---

## 2. Layered Architecture

```
com.srmap.booking
├── BookingApplication.java          # Spring Boot main entry point
├── config/
│   ├── ApiException.java             # Unified HTTP status exception
│   ├── DataInitializer.java         # Database seeding on startup
│   ├── GlobalExceptionHandler.java  # Formats JSON error responses { "message": "..." }
│   ├── SecurityConfig.java          # Security filter chain, CORS & authorization rules
│   └── SecurityErrorHandler.java    # 401 & 403 JSON entry point / access denied handler
├── controller/
│   ├── AdminController.java         # /api/admin/* endpoints
│   ├── AppointmentController.java   # /api/appointments/* endpoints
│   ├── AuthController.java          # /api/auth/* endpoints
│   ├── AvailabilityController.java  # /api/availability/combined endpoint
│   ├── FacultyController.java       # /api/faculty/* endpoints
│   ├── ResourceController.java      # /api/resources/* endpoints
│   └── RoomController.java          # /api/rooms/* endpoints
├── dto/                             # Request and Response Data Transfer Objects
├── entity/                          # JPA Entities with MySQL table mappings
│   ├── Appointment.java
│   ├── Booking.java
│   ├── Resource.java
│   ├── Room.java
│   ├── ScheduleBlock.java
│   ├── User.java
│   └── WorkingHour.java
├── repository/                      # Spring Data JPA repositories with query methods
├── security/                        # JWT Utility, UserDetailsService & OncePerRequestFilter
└── service/                         # Business logic & time interval algorithms
    ├── AdminService.java
    ├── AppointmentService.java
    ├── AuthService.java
    ├── AvailabilityService.java
    ├── BookingService.java
    ├── FacultyService.java
    ├── ResourceService.java
    ├── RoomService.java
    └── TimeIntervalService.java
```

---

## 3. Database Configuration

Configured in `src/main/resources/application.properties`:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:srmap_booking}?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:${MYSQL_USER:root}}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:${MYSQL_PASSWORD:}}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
```

### Environment Variables
You can configure the database via environment variables without modifying source files:
- `MYSQL_PASSWORD` or `SPRING_DATASOURCE_PASSWORD`: Your local MySQL root password.
- `MYSQL_USER` or `SPRING_DATASOURCE_USERNAME`: MySQL user (defaults to `root`).
- `MYSQL_HOST`: MySQL host (defaults to `localhost`).
- `MYSQL_PORT`: MySQL port (defaults to `3306`).
- `MYSQL_DATABASE`: Database name (defaults to `srmap_booking`).

When MySQL runs locally on port 3306, Hibernate creates and updates tables automatically on application launch. Alternatively, you can run `src/main/resources/schema.sql`.

---

## 4. Default Seed Accounts

On initial startup, `DataInitializer` seeds:

| Role | Name | Email | Password |
|---|---|---|---|
| **Admin** | Meera Rao | `admin@srmap.edu.in` | `Campus@123` |
| **Faculty** | Dr. Jayasree Subramanian | `faculty1@srmap.edu.in` | `Campus@123` |
| **Faculty** | Dr. Sugyan Kumar Mishra | `faculty2@srmap.edu.in` | `Campus@123` |
| **Faculty** | (21 additional faculty) | `faculty3`..`8`, `arun.kumar@...` | `Campus@123` |
| **Student** | Priya Sharma | `student1@srmap.edu.in` | `Campus@123` |
| **Student** | Karthik Reddy | `student2@srmap.edu.in` | `Campus@123` |

Also seeded:
- **302 Rooms**: 5 campus-wide sample rooms (A-101, A-204, Meeting Room M1, M2, Seminar Hall S1) plus 297 generated college block rooms across SR Block, V Block, and CV Raman Block.
- **4 Shared Resources**: Portable projector, Laptop cart, Video conferencing kit, 3D printer.
- **Initial Working Hours, Blocked Slots, Appointments & Bookings**.

---

## 5. API Endpoints

### Authentication
- `POST /api/auth/login` - Sign in, returns JWT token & user profile
- `POST /api/auth/register` - Student registration with email and password validations
- `GET /api/auth/me` - Current authenticated user details

### Faculty & Availability
- `GET /api/faculty` - List active faculty
- `GET /api/faculty/{id}/availability?date=YYYY-MM-DD` - Faculty free & busy windows
- `GET /api/faculty/{id}/schedule` - Faculty weekly working hours & blocked intervals
- `PUT /api/faculty/{id}/schedule/working-hours` - Save weekly working hours
- `POST /api/faculty/{id}/schedule/blocks` - Block a specific date/time slot
- `DELETE /api/faculty/{id}/schedule/blocks/{blockId}` - Remove blocked slot

### Rooms & Resources
- `GET /api/rooms` - List active rooms (optional filter by `?type=CLASSROOM`)
- `GET /api/rooms/{id}/availability?date=YYYY-MM-DD` - Room free & busy windows
- `GET /api/resources` - List shared resources
- `GET /api/resources/{id}/availability?date=YYYY-MM-DD` - Resource availability

### Combined Search
- `POST /api/availability/combined` - Intersects faculty calendar with matching available rooms

### Appointments
- `POST /api/appointments` - Request an appointment with faculty
- `GET /api/appointments/mine` - Student's requested appointments
- `GET /api/appointments/incoming` - Faculty's incoming requests
- `PATCH /api/appointments/{id}/status` - Faculty confirms or declines
- `PATCH /api/appointments/{id}/cancel` - Student/Faculty/Admin cancels

### Bookings (Reservations)
- `POST /api/bookings` - Reserve a room and/or resources
- `GET /api/bookings/mine` - User's reservations
- `PATCH /api/bookings/{id}/cancel` - Cancel a reservation

### Admin
- `GET /api/admin/stats` - University-wide dashboard metrics
- `GET /api/admin/appointments` - Review all appointments
- `GET /api/admin/bookings` - Review all bookings
- `GET /api/admin/users` & `POST /api/admin/users` & `PUT /api/admin/users/{id}` - User CRUD
- `GET /api/admin/rooms` & `POST /api/admin/rooms` & `PUT /api/admin/rooms/{id}` - Room CRUD
- `GET /api/admin/resources` & `POST /api/admin/resources` & `PUT /api/admin/resources/{id}` - Resource CRUD

---

## 6. How to Run

```bash
cd backend
mvn clean spring-boot:run
```
Or run `BookingApplication.java` in any Java IDE (IntelliJ IDEA, Eclipse, VS Code).
The backend starts on `http://localhost:8080`.
