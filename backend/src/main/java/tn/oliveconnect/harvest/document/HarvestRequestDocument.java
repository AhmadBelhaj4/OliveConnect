package tn.oliveconnect.harvest.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.enums.HarvestType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Document("harvest_requests")
@Getter @Setter @NoArgsConstructor
public class HarvestRequestDocument {
    @Id private String id;
    @Field(targetType = FieldType.OBJECT_ID) private String farmerId;
    @Field(targetType = FieldType.OBJECT_ID) private String orchardId;
    private String orchardName;
    private HarvestType type;
    private String urgencyReason;
    private HarvestStatus status;
    private Instant requestedAt;
    private LocalDate preferredDate;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal estimatedQuantityKg;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal actualQuantityKg;
    @Field(targetType = FieldType.OBJECT_ID) private String reviewedBy;
    private String reviewNote;
    @Field(targetType = FieldType.OBJECT_ID) private String scheduledTourId;
    private Instant scheduledAt;
    private Instant completedAt;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
