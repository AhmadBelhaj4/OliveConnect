package tn.oliveconnect.harvest.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import tn.oliveconnect.common.enums.HarvestType;

/** farmerId, status, etc. ne sont PAS des champs du record : s'ils sont envoyés, Jackson les ignore. */
public record HarvestRequestCreateRequest(
        @NotBlank(message = "Obligatoire") String orchardId,
        @NotNull(message = "Obligatoire") HarvestType type,
        @NotNull(message = "Obligatoire") LocalDate preferredDate,
        @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") BigDecimal estimatedQuantityKg,
        @Size(max = 500, message = "500 caractères maximum") String urgencyReason) {}
