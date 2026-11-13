package com.srmap.booking.repository;

import com.srmap.booking.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByActiveTrue();
    List<Room> findByActiveTrueAndType(Room.RoomType type);
}
