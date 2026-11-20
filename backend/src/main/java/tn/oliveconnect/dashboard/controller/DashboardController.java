package tn.oliveconnect.dashboard.controller;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.dashboard.dto.DashboardDtos.*;
import tn.oliveconnect.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAnyRole('COLLECTION_MANAGER','PRESS_MANAGER','ADMIN')")
public class DashboardController {

    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(from, to);
    }

    @GetMapping("/yield-by-orchard")
    public YieldByOrchardResponse yieldByOrchard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.yieldByOrchard(from, to);
    }

    @GetMapping("/collection-delays")
    public CollectionDelaysResponse collectionDelays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.collectionDelays(from, to);
    }

    @GetMapping("/bin-fill-rates")
    public BinFillRatesResponse binFillRates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.binFillRates(from, to);
    }
}
