package tn.oliveconnect.harvest.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.harvest.dto.*;
import tn.oliveconnect.harvest.service.HarvestRequestService;
import tn.oliveconnect.security.AuthUser;

@RestController
@RequestMapping("/api/v1/harvest-requests")
public class HarvestRequestController {

    private final HarvestRequestService service;

    public HarvestRequestController(HarvestRequestService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FARMER')")
    public HarvestRequestResponse create(@AuthenticationPrincipal AuthUser me,
                                         @Valid @RequestBody HarvestRequestCreateRequest req) {
        return service.create(me, req);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public PageResponse<HarvestRequestResponse> mine(@AuthenticationPrincipal AuthUser me,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.mine(me, pageable);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER','ADMIN')")
    public PageResponse<HarvestRequestResponse> queue(
            @RequestParam(required = false) HarvestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.queue(status, from, to, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMER','COLLECTION_MANAGER','ADMIN')")
    public HarvestRequestResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return service.get(me, id);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('COLLECTION_MANAGER')")
    public HarvestRequestResponse review(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                         @Valid @RequestBody ReviewRequest req) {
        return service.review(me, id, req);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('FARMER','COLLECTION_MANAGER')")
    public HarvestRequestResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                         @Valid @RequestBody(required = false) CancelRequest req) {
        return service.cancel(me, id, req);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public HarvestRequestResponse patch(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                        @Valid @RequestBody HarvestRequestPatchRequest req) {
        return service.patch(me, id, req);
    }
}
