package tn.oliveconnect.orchard.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.orchard.dto.CreateOrchardRequest;
import tn.oliveconnect.orchard.dto.OrchardResponse;
import tn.oliveconnect.orchard.service.OrchardService;
import tn.oliveconnect.security.UserPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orchards")
@RequiredArgsConstructor
public class OrchardController {

    private final OrchardService orchardService;

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<OrchardResponse> createOrchard(
            @Valid @RequestBody CreateOrchardRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrchardResponse response = orchardService.createOrchard(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrchardResponse>> getOrchards(@AuthenticationPrincipal UserPrincipal principal) {
        List<OrchardResponse> response = orchardService.getOrchards(principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrchardResponse> getOrchardById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrchardResponse response = orchardService.getOrchardById(id, principal);
        return ResponseEntity.ok(response);
    }
}
