package tn.oliveconnect.map.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.enums.MaturityStatus;
import tn.oliveconnect.map.dto.MapDtos.OrchardMapFeatureCollection;
import tn.oliveconnect.map.service.OrchardMapService;
import tn.oliveconnect.security.AuthUser;

@RestController
@RequestMapping("/api/v1/orchards")
public class OrchardMapController {

    private final OrchardMapService service;
    public OrchardMapController(OrchardMapService service) { this.service = service; }

    /** Public ou connecté (périmètre adapté selon le rôle, §6). */
    @GetMapping("/map")
    public OrchardMapFeatureCollection map(
            @AuthenticationPrincipal AuthUser viewer,
            @RequestParam(required = false) String bbox,
            @RequestParam(required = false) MaturityStatus maturity,
            @RequestParam(required = false) String variety,
            @RequestParam(required = false) HarvestStatus harvestStatus) {
        return service.findInBbox(viewer, bbox, maturity, variety, harvestStatus);
    }
}
