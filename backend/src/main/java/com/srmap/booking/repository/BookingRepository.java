package com.srmap.booking.repository;

import com.srmap.booking.entity.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"user", "room", "resources"})
    List<Booking> findByUserIdOrderByDateAscStartTimeAsc(Long userId);

    // All confirmed bookings for a room on a date
    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId AND b.date = :date AND b.status = com.srmap.booking.entity.Booking.Status.CONFIRMED")
    List<Booking> findConfirmedByRoomAndDate(@Param("roomId") Long roomId, @Param("date") LocalDate date);

    // All confirmed bookings that include a specific resource on a date
    @Query("SELECT b FROM Booking b JOIN b.resources r WHERE r.id = :resourceId AND b.date = :date AND b.status = com.srmap.booking.entity.Booking.Status.CONFIRMED")
    List<Booking> findConfirmedByResourceAndDate(@Param("resourceId") Long resourceId, @Param("date") LocalDate date);

    // All bookings ordered by date (admin)
    @EntityGraph(attributePaths = {"user", "room", "resources"})
    List<Booking> findAllByOrderByDateAscStartTimeAsc();

    // Stats
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = com.srmap.booking.entity.Booking.Status.CONFIRMED AND b.date >= :today")
    long countUpcomingConfirmed(@Param("today") LocalDate today);
}
