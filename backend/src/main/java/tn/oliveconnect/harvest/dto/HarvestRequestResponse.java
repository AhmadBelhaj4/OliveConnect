package tn.oliveconnect.harvest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.oliveconnect.harvest.document.HarvestStatus;
import tn.oliveconnect.harvest.document.HarvestType;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarvestRequestResponse {
    private String id;
    private String orchardId;
    private String farmerId;
    private HarvestType type;
    private Double estimatedQuantityKg;
    private LocalDate desiredStartDate;
    private HarvestStatus status;
    private String notes;
    private String rejectionReason;
    private Instant createdAt;
    private Instant updatedAt;
}
