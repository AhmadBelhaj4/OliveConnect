package tn.oliveconnect.orchard.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import tn.oliveconnect.common.enums.MaturityStatus;

import java.time.Instant;

@Document("orchards")
@Getter @Setter @NoArgsConstructor
public class OrchardDocument {
    @Id private String id;
    @Field(targetType = FieldType.OBJECT_ID) private String farmerId;
    private String name;
    private Double areaHectares;
    private String oliveVariety;
    private GeoJsonPoint location;
    private MaturityStatus maturityStatus;
    private boolean publicVisibility;
    private String externalId;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
