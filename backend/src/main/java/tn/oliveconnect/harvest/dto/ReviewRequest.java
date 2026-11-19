package tn.oliveconnect.harvest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull(message = "Obligatoire") ReviewDecision decision,
        @Size(max = 500, message = "500 caractères maximum") String reason) {}
