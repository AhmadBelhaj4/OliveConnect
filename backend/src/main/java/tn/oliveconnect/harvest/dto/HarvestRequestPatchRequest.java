package tn.oliveconnect.harvest.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import tn.oliveconnect.common.enums.HarvestType;

/** PATCH : même corps que la création, tous les champs optionnels ; orchardId non modifiable. */
public record HarvestRequestPatchRequest(
        HarvestType type,
        LocalDate preferredDate,
        @Positive(message = "Doit être > 0") BigDecimal estimatedQuantityKg,
        @Size(max = 500, message = "500 caractères maximum") String urgencyReason) {}
