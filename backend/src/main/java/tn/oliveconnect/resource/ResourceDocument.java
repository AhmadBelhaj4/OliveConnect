package tn.oliveconnect.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "resources")
public class ResourceDocument {
    @Id
    private String id;
    private String name;
    private String type; // VEHICLE, EQUIPMENT, BIN
    private String status; // AVAILABLE, IN_USE, MAINTENANCE
    private String externalId;
}
