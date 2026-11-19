package tn.oliveconnect.resource.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.common.enums.VehicleType;

@Document("vehicles")
@Getter @Setter @NoArgsConstructor
public class VehicleDocument {
    @Id private String id;
    private VehicleType type;
    private String registration;
    private Integer capacityKg;
    private ResourceStatus status;
}
