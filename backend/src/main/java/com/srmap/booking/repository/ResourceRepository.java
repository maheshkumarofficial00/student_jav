package com.srmap.booking.repository;

import com.srmap.booking.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByActiveTrue();
}
