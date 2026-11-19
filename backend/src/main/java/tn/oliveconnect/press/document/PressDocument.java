package tn.oliveconnect.press.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("presses")
@Getter @Setter @NoArgsConstructor
public class PressDocument {
    @Id private String id;
    private String name;
    private GeoJsonPoint location;
    private Integer dailyCapacityKg;
    private Integer availableCapacityKg;
    private String externalId;
}
