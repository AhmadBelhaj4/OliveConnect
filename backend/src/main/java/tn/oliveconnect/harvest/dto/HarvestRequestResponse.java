package tn.oliveconnect.harvest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.enums.HarvestType;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;

/** Contrat §5.5 HarvestRequestResponse — 18 clés exactement. */
public record HarvestRequestResponse(
        String id,
        String farmerId,
        String orchardId,
        String orchardName,
        HarvestType type,
        String urgencyReason,
        HarvestStatus status,
        Instant requestedAt,
        LocalDate preferredDate,
        BigDecimal estimatedQuantityKg,
        BigDecimal actualQuantityKg,
        String reviewedBy,
        String reviewNote,
        String scheduledTourId,
        Instant scheduledAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static HarvestRequestResponse from(HarvestRequestDocument d) {
        return new HarvestRequestResponse(
                d.getId(),
                d.getFarmerId(),
                d.getOrchardId(),
                d.getOrchardName(),
                d.getType(),
                d.getUrgencyReason(),
                d.getStatus(),
                d.getRequestedAt(),
                d.getPreferredDate(),
                d.getEstimatedQuantityKg(),
                d.getActualQuantityKg(),
                d.getReviewedBy(),
                d.getReviewNote(),
                d.getScheduledTourId(),
                d.getScheduledAt(),
                d.getCompletedAt(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
