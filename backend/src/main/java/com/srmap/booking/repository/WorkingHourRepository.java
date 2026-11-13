package com.srmap.booking.repository;

import com.srmap.booking.entity.WorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkingHourRepository extends JpaRepository<WorkingHour, Long> {
    List<WorkingHour> findByFacultyId(Long facultyId);
    void deleteByFacultyId(Long facultyId);
}
