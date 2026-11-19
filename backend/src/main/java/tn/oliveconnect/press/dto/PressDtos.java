package tn.oliveconnect.press.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import tn.oliveconnect.common.enums.QualityGrade;
import tn.oliveconnect.common.web.GeoPointDto;
import tn.oliveconnect.press.document.*;

public final class PressDtos {
    private PressDtos() {}

    public record PressCapacityPatchRequest(
            @NotNull(message = "Obligatoire") @Min(value = 0, message = "Doit être >= 0") Integer availableCapacityKg) {}

    /** Contrat §5.5 Press — 5 clés exactement. */
    public record PressResponse(String id, String name, GeoPointDto location,
                                Integer dailyCapacityKg, Integer availableCapacityKg) {
        public static PressResponse from(PressDocument p) {
            return new PressResponse(p.getId(), p.getName(), GeoPointDto.from(p.getLocation()),
                    p.getDailyCapacityKg(), p.getAvailableCapacityKg());
        }
    }

    public record PressReceptionCreateRequest(
            @NotBlank(message = "Obligatoire") String tourId,
            @NotBlank(message = "Obligatoire") String binId,
            @NotBlank(message = "Obligatoire") String pressId,
            @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") BigDecimal weightKg,
            @NotNull(message = "Obligatoire") QualityGrade qualityGrade,
            @Size(max = 500) String notes,
            Instant receivedAt) {}

    /** Contrat §5.5 PressReceptionResponse — 9 clés exactement. */
    public record PressReceptionResponse(
            String id, String tourId, String binId, String pressId, Instant receivedAt,
            BigDecimal weightKg, QualityGrade qualityGrade, String notes, String recordedBy) {
        public static PressReceptionResponse from(PressReceptionDocument r) {
            return new PressReceptionResponse(r.getId(), r.getTourId(), r.getBinId(), r.getPressId(),
                    r.getReceivedAt(), r.getWeightKg(), r.getQualityGrade(), r.getNotes(), r.getRecordedBy());
        }
    }
}
