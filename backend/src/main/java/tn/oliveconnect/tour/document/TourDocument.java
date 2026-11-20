package tn.oliveconnect.tour.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import tn.oliveconnect.common.enums.StopStatus;
import tn.oliveconnect.common.enums.TourStatus;

@Document("tours")
@Getter @Setter @NoArgsConstructor
public class TourDocument {
    @Id private String id;
    private LocalDate tourDate;
    private TourStatus status;
    private String teamLeaderId;
    private List<String> vehicleIds = new ArrayList<>();
    private List<String> workerIds = new ArrayList<>();
    private List<String> binIds = new ArrayList<>();
    private List<TourStop> stops = new ArrayList<>();   // imbriqué, borné <= 20 (§3.2, B1)
    private Instant startedAt;
    private Instant completedAt;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    @Getter @Setter @NoArgsConstructor
    public static class TourStop {
        private String stopId;
        private int sequence;
        private String harvestRequestId;
        private String farmerId;                        // interne : pour notifier l'agriculteur sans requête de plus
        private String orchardId;
        private String orchardName;
        @Field(targetType = FieldType.DECIMAL128) private BigDecimal plannedQuantityKg;
        @Field(targetType = FieldType.DECIMAL128) private BigDecimal actualQuantityKg;
        private StopStatus status;
        private String note;                            // interne
    }
}
