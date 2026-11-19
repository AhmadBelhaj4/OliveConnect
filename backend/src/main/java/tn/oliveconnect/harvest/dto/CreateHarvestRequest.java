package tn.oliveconnect.harvest.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.oliveconnect.harvest.document.HarvestType;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHarvestRequest {
    @NotBlank(message = "L'identifiant du verger est obligatoire")
    private String orchardId;

    @NotNull(message = "Le type de récolte est obligatoire")
    private HarvestType type;

    @NotNull(message = "La quantité estimée est obligatoire")
    @Positive(message = "La quantité estimée doit être strictement positive")
    private Double estimatedQuantityKg;

    @NotNull(message = "La date souhaitée est obligatoire")
    private LocalDate desiredStartDate;

    private String notes;
}
