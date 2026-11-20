package tn.oliveconnect.tour.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import tn.oliveconnect.common.enums.StopStatus;
import tn.oliveconnect.common.enums.TourStatus;
import tn.oliveconnect.tour.document.TourDocument;

public final class TourDtos {
    private TourDtos() {}

    public record TourCreateRequest(@NotNull(message = "Obligatoire") LocalDate tourDate) {}

    public record PlanStopRequest(
            @NotBlank(message = "Obligatoire") String harvestRequestId,
            @Min(value = 1, message = "Doit être >= 1") int sequence) {}

    /** Bornes figées §5.5 : 20 arrêts, 30 ouvriers, 10 véhicules, 20 bennes. */
    public record TourPlanRequest(
            @NotBlank(message = "Obligatoire") String teamLeaderId,
            @NotNull @Size(min = 1, max = 10, message = "1 à 10 véhicules") List<@NotBlank String> vehicleIds,
            @NotNull @Size(min = 1, max = 30, message = "1 à 30 ouvriers") List<@NotBlank String> workerIds,
            @NotNull @Size(min = 1, max = 20, message = "1 à 20 bennes") List<@NotBlank String> binIds,
            @NotNull @Size(min = 1, max = 20, message = "1 à 20 arrêts") List<@Valid PlanStopRequest> stops) {}

    public record StopCompleteRequest(
            @NotNull(message = "Obligatoire") @Positive(message = "Doit être > 0") BigDecimal actualQuantityKg,
            @Size(max = 500) String note) {}

    /** Contrat §5.5 : stopId, sequence, harvestRequestId, orchardId, orchardName, plannedQuantityKg, actualQuantityKg, status. */
    public record TourStopResponse(
            String stopId, int sequence, String harvestRequestId, String orchardId, String orchardName,
            BigDecimal plannedQuantityKg, BigDecimal actualQuantityKg, StopStatus status) {
        public static TourStopResponse from(TourDocument.TourStop s) {
            return new TourStopResponse(s.getStopId(), s.getSequence(), s.getHarvestRequestId(),
                    s.getOrchardId(), s.getOrchardName(), s.getPlannedQuantityKg(), s.getActualQuantityKg(), s.getStatus());
        }
    }

    /** Contrat §5.5 TourResponse — 13 clés exactement. */
    public record TourResponse(
            String id, LocalDate tourDate, TourStatus status, String teamLeaderId,
            List<String> vehicleIds, List<String> workerIds, List<String> binIds,
            List<TourStopResponse> stops, Instant startedAt, Instant completedAt,
            Instant createdAt, Instant updatedAt) {
        public static TourResponse from(TourDocument t) {
            return new TourResponse(t.getId(), t.getTourDate(), t.getStatus(), t.getTeamLeaderId(),
                    t.getVehicleIds(), t.getWorkerIds(), t.getBinIds(),
                    t.getStops().stream().map(TourStopResponse::from).toList(),
                    t.getStartedAt(), t.getCompletedAt(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }
}
