package tn.oliveconnect.resource.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.oliveconnect.common.enums.EquipmentType;
import tn.oliveconnect.common.enums.ResourceStatus;

@Document("equipment")
@Getter @Setter @NoArgsConstructor
public class EquipmentDocument {
    @Id private String id;
    private EquipmentType type;
    private String label;
    private Integer quantity;
    private ResourceStatus status;
}
