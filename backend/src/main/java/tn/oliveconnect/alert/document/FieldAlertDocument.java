package tn.oliveconnect.alert.document;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import tn.oliveconnect.common.enums.AlertPriority;
import tn.oliveconnect.common.enums.AlertStatus;
import tn.oliveconnect.common.enums.AlertType;

@Document("field_alerts")
@Getter @Setter @NoArgsConstructor
public class FieldAlertDocument {
    @Id private String id;
    @Field(targetType = FieldType.OBJECT_ID) private String farmerId;
    @Field(targetType = FieldType.OBJECT_ID) private String orchardId;
    private AlertType type;
    private String description;
    private GeoJsonPoint location;     // [lon, lat]
    private AlertPriority priority;
    private AlertStatus status;
    @CreatedDate private Instant createdAt;
    private Instant acknowledgedAt;
    private Instant resolvedAt;        // renseigné pour RESOLVED et DISMISSED (§5.5)
    private String resolutionNote;     // stocke la note (resolve) ou le motif (dismiss)
}
