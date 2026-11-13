package com.srmap.booking;

import com.srmap.booking.dto.ResourceDto;
import com.srmap.booking.dto.ResourceRequest;
import com.srmap.booking.dto.RoomDto;
import com.srmap.booking.dto.RoomRequest;
import com.srmap.booking.service.ResourceService;
import com.srmap.booking.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RoomAndResourceServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private ResourceService resourceService;

    @Test
    void testRoomLifecycle() {
        // 1. Get active rooms
        List<RoomDto> activeRooms = roomService.getActiveRooms(null);
        assertNotNull(activeRooms);
        assertFalse(activeRooms.isEmpty());

        // 2. Filter by type
        List<RoomDto> classrooms = roomService.getActiveRooms("CLASSROOM");
        assertNotNull(classrooms);
        assertFalse(classrooms.isEmpty());
        assertTrue(classrooms.stream().allMatch(r -> "CLASSROOM".equals(r.getType())));

        // 3. Create room
        RoomRequest createReq = new RoomRequest();
        createReq.setName("Test Conference Room");
        createReq.setBlock("SR Block");
        createReq.setFloor(3);
        createReq.setType("MEETING_ROOM");
        createReq.setCapacity(25);
        createReq.setFeatures(List.of("projector", "smartboard"));
        createReq.setActive(true);

        RoomDto created = roomService.createRoom(createReq);
        assertNotNull(created.getId());
        assertEquals("Test Conference Room", created.getName());
        assertEquals("SR Block, Floor 3", created.getBuilding());
        assertEquals("MEETING_ROOM", created.getType());

        // 4. Update room
        RoomRequest updateReq = new RoomRequest();
        updateReq.setName("Updated Conference Room");
        updateReq.setBlock("SR Block");
        updateReq.setFloor(4);
        updateReq.setType("MEETING_ROOM");
        updateReq.setCapacity(30);
        updateReq.setFeatures(List.of("projector"));
        updateReq.setActive(true);

        RoomDto updated = roomService.updateRoom(created.getId(), updateReq);
        assertEquals("Updated Conference Room", updated.getName());
        assertEquals(30, updated.getCapacity());
        assertEquals("SR Block, Floor 4", updated.getBuilding());
    }

    @Test
    void testResourceLifecycle() {
        // 1. Get active resources
        List<ResourceDto> active = resourceService.getActiveResources();
        assertNotNull(active);
        assertFalse(active.isEmpty());

        // 2. Create resource
        ResourceRequest createReq = new ResourceRequest();
        createReq.setName("Digital Drawing Tablet");
        createReq.setCategory("Computing");
        createReq.setDescription("Wacom tablet for digital art");
        createReq.setActive(true);

        ResourceDto created = resourceService.createResource(createReq);
        assertNotNull(created.getId());
        assertEquals("Digital Drawing Tablet", created.getName());

        // 3. Update resource
        ResourceRequest updateReq = new ResourceRequest();
        updateReq.setName("Wacom Cintiq Tablet");
        updateReq.setCategory("Design Equipment");
        updateReq.setDescription("16-inch display drawing tablet");
        updateReq.setActive(true);

        ResourceDto updated = resourceService.updateResource(created.getId(), updateReq);
        assertEquals("Wacom Cintiq Tablet", updated.getName());
        assertEquals("Design Equipment", updated.getCategory());
    }
}
