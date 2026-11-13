package com.srmap.booking.repository;

import com.srmap.booking.entity.ScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, Long> {
    List<ScheduleBlock> findByFacultyId(Long facultyId);
    List<ScheduleBlock> findByFacultyIdAndDate(Long facultyId, LocalDate date);
}
