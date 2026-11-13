package com.srmap.booking.service;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.RoomDto;
import com.srmap.booking.dto.RoomRequest;
import com.srmap.booking.entity.Room;
import com.srmap.booking.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomDto> getActiveRooms(String typeStr) {
        if (typeStr != null && !typeStr.isBlank()) {
            try {
                Room.RoomType type = Room.RoomType.valueOf(typeStr.trim().toUpperCase());
                return roomRepository.findByActiveTrueAndType(type).stream().map(RoomDto::from).toList();
            } catch (IllegalArgumentException ignored) {
                return List.of();
            }
        }
        return roomRepository.findByActiveTrue().stream().map(RoomDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RoomDto> getAllRooms() {
        return roomRepository.findAll().stream().map(RoomDto::from).toList();
    }

    @Transactional
    public RoomDto createRoom(RoomRequest req) {
        validateRoomRequest(req);

        Room.RoomType type;
        try {
            type = Room.RoomType.valueOf(req.getType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Please fill in all required fields.");
        }

        Room room = Room.builder()
                .name(req.getName().trim())
                .building(req.getBuilding())
                .block(req.getBlock())
                .floor(req.getFloor())
                .type(type)
                .capacity(req.getCapacity())
                .features(req.getFeatures() != null ? String.join(",", req.getFeatures()) : "")
                .active(req.getActive() == null || req.getActive())
                .build();

        applyLocation(room);
        room = roomRepository.save(room);
        return RoomDto.from(room);
    }

    @Transactional
    public RoomDto updateRoom(Long id, RoomRequest req) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Not found."));

        validateRoomRequest(req);

        Room.RoomType type;
        try {
            type = Room.RoomType.valueOf(req.getType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Please fill in all required fields.");
        }

        room.setName(req.getName().trim());
        room.setBuilding(req.getBuilding());
        room.setBlock(req.getBlock());
        room.setFloor(req.getFloor());
        room.setType(type);
        room.setCapacity(req.getCapacity());
        if (req.getFeatures() != null) {
            room.setFeatures(String.join(",", req.getFeatures()));
        }
        if (req.getActive() != null) {
            room.setActive(req.getActive());
        }

        applyLocation(room);
        room = roomRepository.save(room);
        return RoomDto.from(room);
    }

    private void validateRoomRequest(RoomRequest req) {
        if (req.getName() == null || req.getName().isBlank() ||
                req.getType() == null || req.getType().isBlank() ||
                req.getCapacity() == null || req.getCapacity() <= 0) {
            throw ApiException.badRequest("Please fill in all required fields.");
        }
    }

    private void applyLocation(Room room) {
        if (room.getBlock() == null || room.getBlock().isBlank()) {
            room.setBlock(null);
            room.setFloor(null);
            return;
        }
        if (room.getFloor() != null) {
            room.setBuilding(room.getBlock() + ", Floor " + room.getFloor());
        }
    }
}
