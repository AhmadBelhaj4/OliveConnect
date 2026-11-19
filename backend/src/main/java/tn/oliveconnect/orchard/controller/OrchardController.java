package tn.oliveconnect.orchard.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.orchard.dto.*;
import tn.oliveconnect.orchard.service.OrchardService;
import tn.oliveconnect.security.AuthUser;

@RestController
@RequestMapping("/api/v1/orchards")
public class OrchardController {

    private final OrchardService service;

    public OrchardController(OrchardService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FARMER')")
    public OrchardResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody OrchardCreateRequest req) {
        return service.create(me, req);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public PageResponse<OrchardResponse> mine(@AuthenticationPrincipal AuthUser me,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.mine(me, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMER','COLLECTION_MANAGER','ADMIN')")
    public OrchardResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return service.get(me, id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMER','COLLECTION_MANAGER','ADMIN')")
    public OrchardResponse patch(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                 @Valid @RequestBody OrchardPatchRequest req) {
        return service.patch(me, id, req);
    }
}
