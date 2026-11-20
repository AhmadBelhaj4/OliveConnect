package tn.oliveconnect.alert.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.alert.dto.AlertDtos.*;
import tn.oliveconnect.alert.service.FieldAlertService;
import tn.oliveconnect.common.enums.AlertPriority;
import tn.oliveconnect.common.enums.AlertStatus;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.security.AuthUser;

@RestController
@RequestMapping("/api/v1/alerts")
public class FieldAlertController {

    private final FieldAlertService service;
    public FieldAlertController(FieldAlertService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FARMER')")
    public FieldAlertResponse create(@AuthenticationPrincipal AuthUser me,
                                     @Valid @RequestBody FieldAlertCreateRequest req) {
        return service.create(me, req);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FARMER','COLLECTION_MANAGER','ADMIN')")
    public PageResponse<FieldAlertResponse> list(@AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertPriority priority,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(me, status, priority, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMER','COLLECTION_MANAGER','ADMIN')")
    public FieldAlertResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return service.get(me, id);
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('COLLECTION_MANAGER')")
    public FieldAlertResponse acknowledge(@PathVariable String id) { return service.acknowledge(id); }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('COLLECTION_MANAGER')")
    public FieldAlertResponse resolve(@PathVariable String id, @Valid @RequestBody ResolveAlertRequest req) {
        return service.resolve(id, req);
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("hasRole('COLLECTION_MANAGER')")
    public FieldAlertResponse dismiss(@PathVariable String id, @Valid @RequestBody DismissAlertRequest req) {
        return service.dismiss(id, req);
    }
}
