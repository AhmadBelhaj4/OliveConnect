package tn.oliveconnect.resource.document;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.oliveconnect.common.enums.ResourceStatus;

@Document("workers")
@Getter @Setter @NoArgsConstructor
public class WorkerDocument {
    @Id private String id;
    private String fullName;
    private String phone;
    private List<String> skills;
    private ResourceStatus status;
}
