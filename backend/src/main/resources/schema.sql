-- ============================================================
-- Smart Faculty and Resource Booking System – MySQL Schema
-- ============================================================

-- Users table (Students, Faculty, Admins)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    department VARCHAR(150),
    office VARCHAR(100),
    title VARCHAR(100),
    subjects VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Working Hours for Faculty
CREATE TABLE IF NOT EXISTS working_hours (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    faculty_id BIGINT NOT NULL,
    day_of_week INT NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    CONSTRAINT fk_wh_faculty FOREIGN KEY (faculty_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Blocked schedule slots for Faculty
CREATE TABLE IF NOT EXISTS schedule_blocks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    faculty_id BIGINT NOT NULL,
    date DATE NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    reason VARCHAR(150),
    CONSTRAINT fk_block_faculty FOREIGN KEY (faculty_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Rooms (Classrooms, Meeting rooms, Seminar halls)
CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    building VARCHAR(150),
    block VARCHAR(80),
    floor INT,
    type VARCHAR(20) NOT NULL,
    capacity INT NOT NULL,
    features VARCHAR(300),
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Resources (Shared academic equipment)
CREATE TABLE IF NOT EXISTS resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100),
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Appointments (Student - Faculty)
CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    faculty_id BIGINT NOT NULL,
    room_id BIGINT,
    date DATE NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    status VARCHAR(15) NOT NULL,
    CONSTRAINT fk_appt_student FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_appt_faculty FOREIGN KEY (faculty_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_appt_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bookings (Room / Resource reservations)
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    room_id BIGINT,
    date DATE NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    status VARCHAR(15) NOT NULL,
    CONSTRAINT fk_book_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_book_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Booking to Resources Join Table
CREATE TABLE IF NOT EXISTS booking_resources (
    booking_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    PRIMARY KEY (booking_id, resource_id),
    CONSTRAINT fk_br_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_br_resource FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
