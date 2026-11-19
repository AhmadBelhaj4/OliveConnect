package tn.oliveconnect.harvest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.harvest.dto.CreateHarvestRequest;
import tn.oliveconnect.harvest.dto.HarvestRequestResponse;
import tn.oliveconnect.harvest.service.HarvestRequestService;
import tn.oliveconnect.security.UserPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/v1/harvest-requests")
@RequiredArgsConstructor
public class HarvestRequestController {

    private final HarvestRequestService harvestRequestService;

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<HarvestRequestResponse> createHarvestRequest(
            @Valid @RequestBody CreateHarvestRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        HarvestRequestResponse response = harvestRequestService.createHarvestRequest(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<HarvestRequestResponse>> getHarvestRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<HarvestRequestResponse> response = harvestRequestService.getHarvestRequests(principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HarvestRequestResponse> getHarvestRequestById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        HarvestRequestResponse response = harvestRequestService.getHarvestRequestById(id, principal);
        return ResponseEntity.ok(response);
    }
}
