package tn.oliveconnect.orchard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import tn.oliveconnect.common.web.GeoPointDto;

public record OrchardCreateRequest(
        @NotBlank(message = "Obligatoire") @Size(max = 100, message = "100 caractères maximum") String name,
        @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") Double areaHectares,
        @NotBlank(message = "Obligatoire") @Size(max = 50) String oliveVariety,
        @NotNull(message = "Obligatoire") @Valid GeoPointDto location,
        @NotNull(message = "Obligatoire") Boolean publicVisibility) {}
