package tn.oliveconnect.resource.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.oliveconnect.common.enums.ResourceStatus;

@Document("bins")
@Getter @Setter @NoArgsConstructor
public class BinDocument {
    @Id private String id;
    private String code;
    private Integer capacityKg;
    private ResourceStatus status;
}
