package tn.oliveconnect.orchard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.oliveconnect.orchard.document.GeoLocation;
import tn.oliveconnect.orchard.document.MaturityStatus;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrchardResponse {
    private String id;
    private String farmerId;
    private String name;
    private Double surfaceHectares;
    private Integer treeCount;
    private String oliveVariety;
    private GeoLocation location;
    private MaturityStatus maturityStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
