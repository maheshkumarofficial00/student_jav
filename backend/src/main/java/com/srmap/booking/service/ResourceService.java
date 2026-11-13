package com.srmap.booking.service;

import com.srmap.booking.config.ApiException;
import com.srmap.booking.dto.ResourceDto;
import com.srmap.booking.dto.ResourceRequest;
import com.srmap.booking.entity.Resource;
import com.srmap.booking.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public List<ResourceDto> getActiveResources() {
        return resourceRepository.findByActiveTrue().stream().map(ResourceDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceDto> getAllResources() {
        return resourceRepository.findAll().stream().map(ResourceDto::from).toList();
    }

    @Transactional
    public ResourceDto createResource(ResourceRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw ApiException.badRequest("Please fill in all required fields.");
        }

        Resource res = Resource.builder()
                .name(req.getName().trim())
                .category(req.getCategory())
                .description(req.getDescription())
                .active(req.getActive() == null || req.getActive())
                .build();

        res = resourceRepository.save(res);
        return ResourceDto.from(res);
    }

    @Transactional
    public ResourceDto updateResource(Long id, ResourceRequest req) {
        Resource res = resourceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Not found."));

        if (req.getName() == null || req.getName().isBlank()) {
            throw ApiException.badRequest("Please fill in all required fields.");
        }

        res.setName(req.getName().trim());
        res.setCategory(req.getCategory());
        res.setDescription(req.getDescription());
        if (req.getActive() != null) {
            res.setActive(req.getActive());
        }

        res = resourceRepository.save(res);
        return ResourceDto.from(res);
    }
}
