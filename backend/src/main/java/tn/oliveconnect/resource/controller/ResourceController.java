package tn.oliveconnect.resource.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.resource.service.ResourceService;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final ResourceService service;

    public ResourceController(ResourceService service) {
        this.service = service;
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','COLLECTION_MANAGER','ADMIN')")
    public tn.oliveconnect.resource.dto.ResourceDtos.ResourceAvailabilityResponse availability(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate date) {
        return service.availability(date);
    }

    @GetMapping("/{kind}")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','COLLECTION_MANAGER','ADMIN')")
    public PageResponse<?> list(@PathVariable String kind,
                                @RequestParam(required = false) ResourceStatus status,
                                @PageableDefault(size = 20) Pageable pageable) {
        return service.list(kind, status, pageable);
    }

    @PostMapping("/{kind}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','ADMIN')")
    public Object create(@PathVariable String kind, @RequestBody JsonNode body) {
        return service.create(kind, body);
    }

    @PatchMapping("/{kind}/{id}")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','ADMIN')")
    public Object patch(@PathVariable String kind, @PathVariable String id, @RequestBody JsonNode body) {
        return service.patch(kind, id, body);
    }
}
