package tn.oliveconnect.orchard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.oliveconnect.orchard.document.GeoLocation;
import tn.oliveconnect.orchard.document.MaturityStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrchardRequest {
    @NotBlank(message = "Le nom du verger est obligatoire")
    private String name;

    @NotNull(message = "La surface est obligatoire")
    @Positive(message = "La surface doit être strictement positive")
    private Double surfaceHectares;

    @NotNull(message = "Le nombre d'arbres est obligatoire")
    @Positive(message = "Le nombre d'arbres doit être strictement positif")
    private Integer treeCount;

    @NotBlank(message = "La variété d'olives est obligatoire")
    private String oliveVariety;

    @Valid
    @NotNull(message = "La localisation est obligatoire")
    private GeoLocation location;

    private MaturityStatus maturityStatus;
}
