package tn.oliveconnect.harvest.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "harvest_requests")
public class HarvestRequestDocument {
    @Id
    private String id;

    @Indexed
    private String orchardId;

    @Indexed
    private String farmerId;

    private HarvestType type;

    private Double estimatedQuantityKg;

    private LocalDate desiredStartDate;

    @Indexed
    @Builder.Default
    private HarvestStatus status = HarvestStatus.SUBMITTED;

    private String notes;

    private String rejectionReason;

    private String externalId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
