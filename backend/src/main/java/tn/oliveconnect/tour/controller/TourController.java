package tn.oliveconnect.tour.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.security.AuthUser;
import tn.oliveconnect.tour.dto.TourDtos.*;
import tn.oliveconnect.tour.service.TourService;

@RestController
@RequestMapping("/api/v1/tours")
public class TourController {

    private final TourService service;
    public TourController(TourService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COLLECTION_MANAGER')")
    public TourResponse create(@Valid @RequestBody TourCreateRequest req) { return service.create(req); }

    @GetMapping
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER','LOGISTICS_MANAGER','ADMIN','TEAM_LEADER')")
    public PageResponse<TourResponse> list(@AuthenticationPrincipal AuthUser me,
            @PageableDefault(size = 20, sort = "tourDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(me, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COLLECTION_MANAGER','LOGISTICS_MANAGER','ADMIN','TEAM_LEADER')")
    public TourResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return service.get(me, id);
    }

    @PostMapping("/{id}/plan")
    @PreAuthorize("hasRole('COLLECTION_MANAGER')")
    public TourResponse plan(@PathVariable String id, @Valid @RequestBody TourPlanRequest req) {
        return service.plan(id, req);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('TEAM_LEADER','COLLECTION_MANAGER')")
    public TourResponse start(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return service.start(me, id);
    }

    @PostMapping("/{id}/stops/{stopId}/complete")
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public TourResponse completeStop(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                     @PathVariable String stopId, @Valid @RequestBody StopCompleteRequest req) {
        return service.completeStop(me, id, stopId, req);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('TEAM_LEADER','COLLECTION_MANAGER')")
    public TourResponse complete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return service.complete(me, id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('COLLECTION_MANAGER')")
    public TourResponse cancel(@PathVariable String id) { return service.cancel(id); }
}
