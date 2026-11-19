package tn.oliveconnect.orchard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tn.oliveconnect.common.enums.MaturityStatus;
import tn.oliveconnect.common.web.GeoPointDto;

public record OrchardPatchRequest(
        @Size(min = 1, max = 100, message = "1 à 100 caractères") String name,
        @Positive(message = "Doit être > 0") Double areaHectares,
        @Size(min = 1, max = 50) String oliveVariety,
        @Valid GeoPointDto location,
        Boolean publicVisibility,
        MaturityStatus maturityStatus) {}
