package com.srmap.booking.repository;

import com.srmap.booking.entity.Appointment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"student", "faculty", "room"})
    List<Appointment> findByStudentIdOrderByDateAscStartTimeAsc(Long studentId);

    @EntityGraph(attributePaths = {"student", "faculty", "room"})
    List<Appointment> findByFacultyIdOrderByDateAscStartTimeAsc(Long facultyId);

    // All active (PENDING or CONFIRMED) appointments for a faculty on a date
    @Query("SELECT a FROM Appointment a WHERE a.faculty.id = :facultyId AND a.date = :date AND a.status IN (com.srmap.booking.entity.Appointment.Status.PENDING, com.srmap.booking.entity.Appointment.Status.CONFIRMED)")
    List<Appointment> findActiveByFacultyAndDate(@Param("facultyId") Long facultyId, @Param("date") LocalDate date);

    // All active appointments for a room on a date
    @Query("SELECT a FROM Appointment a WHERE a.room.id = :roomId AND a.date = :date AND a.status IN (com.srmap.booking.entity.Appointment.Status.PENDING, com.srmap.booking.entity.Appointment.Status.CONFIRMED)")
    List<Appointment> findActiveByRoomAndDate(@Param("roomId") Long roomId, @Param("date") LocalDate date);

    // For overlap check: does the faculty have an active appointment overlapping [start, end) on this date?
    @Query("SELECT a FROM Appointment a WHERE a.faculty.id = :facultyId AND a.date = :date " +
           "AND a.status IN (com.srmap.booking.entity.Appointment.Status.PENDING, com.srmap.booking.entity.Appointment.Status.CONFIRMED) AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findOverlappingForFaculty(@Param("facultyId") Long facultyId, @Param("date") LocalDate date,
                                                 @Param("startTime") String startTime, @Param("endTime") String endTime);

    // Admin: all appointments ordered by date
    @EntityGraph(attributePaths = {"student", "faculty", "room"})
    List<Appointment> findAllByOrderByDateAscStartTimeAsc();

    // Stats
    long countByStatus(Appointment.Status status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status IN (com.srmap.booking.entity.Appointment.Status.PENDING, com.srmap.booking.entity.Appointment.Status.CONFIRMED) AND a.date >= :today")
    long countUpcoming(@Param("today") LocalDate today);
}
