package tn.oliveconnect.alert.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import tn.oliveconnect.alert.document.FieldAlertDocument;
import tn.oliveconnect.common.enums.*;
import tn.oliveconnect.common.web.GeoPointDto;

public final class AlertDtos {
    private AlertDtos() {}

    public record FieldAlertCreateRequest(
            @NotBlank(message = "Obligatoire") String orchardId,
            @NotNull(message = "Obligatoire") AlertType type,
            @NotBlank(message = "Obligatoire") @Size(max = 1000) String description,
            @NotNull(message = "Obligatoire") @Valid GeoPointDto location,
            @NotNull(message = "Obligatoire") AlertPriority priority) {}

    public record ResolveAlertRequest(
            @NotBlank(message = "Note obligatoire") @Size(max = 1000) String note) {}

    public record DismissAlertRequest(
            @NotBlank(message = "Motif obligatoire") @Size(max = 1000) String reason) {}

    /** Contrat §5.5 FieldAlertResponse — 12 clés exactement. */
    public record FieldAlertResponse(
            String id, String farmerId, String orchardId, AlertType type, String description,
            GeoPointDto location, AlertPriority priority, AlertStatus status,
            Instant createdAt, Instant acknowledgedAt, Instant resolvedAt, String resolutionNote) {

        public static FieldAlertResponse from(FieldAlertDocument a) {
            return new FieldAlertResponse(a.getId(), a.getFarmerId(), a.getOrchardId(), a.getType(),
                    a.getDescription(), GeoPointDto.from(a.getLocation()), a.getPriority(), a.getStatus(),
                    a.getCreatedAt(), a.getAcknowledgedAt(), a.getResolvedAt(), a.getResolutionNote());
        }
    }
}
