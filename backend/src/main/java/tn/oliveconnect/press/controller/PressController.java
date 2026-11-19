package tn.oliveconnect.press.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.press.dto.PressDtos.*;
import tn.oliveconnect.press.service.PressService;
import tn.oliveconnect.security.AuthUser;

@RestController
@RequestMapping("/api/v1")
public class PressController {

    private final PressService service;
    public PressController(PressService service) { this.service = service; }

    @GetMapping("/presses")
    @PreAuthorize("hasAnyRole('PRESS_MANAGER','COLLECTION_MANAGER','ADMIN')")
    public PageResponse<PressResponse> listPresses(@PageableDefault(size = 20) Pageable pageable) {
        return service.listPresses(pageable);
    }

    @PatchMapping("/presses/{id}/capacity")
    @PreAuthorize("hasRole('PRESS_MANAGER')")
    public PressResponse updateCapacity(@PathVariable String id, @Valid @RequestBody PressCapacityPatchRequest req) {
        return service.updateCapacity(id, req);
    }

    @PostMapping("/press-receptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PRESS_MANAGER')")
    public PressReceptionResponse record(@AuthenticationPrincipal AuthUser me,
                                         @Valid @RequestBody PressReceptionCreateRequest req) {
        return service.recordReception(me, req);
    }

    @GetMapping("/press-receptions")
    @PreAuthorize("hasAnyRole('PRESS_MANAGER','COLLECTION_MANAGER','ADMIN')")
    public PageResponse<PressReceptionResponse> listReceptions(
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listReceptions(pageable);
    }
}
