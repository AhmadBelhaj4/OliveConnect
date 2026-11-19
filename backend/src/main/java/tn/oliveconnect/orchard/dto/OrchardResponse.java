package tn.oliveconnect.orchard.dto;

import java.time.Instant;
import tn.oliveconnect.common.enums.MaturityStatus;
import tn.oliveconnect.common.web.GeoPointDto;
import tn.oliveconnect.orchard.document.OrchardDocument;

/** Contrat §5.5 OrchardResponse — 10 clés exactement. */
public record OrchardResponse(
        String id,
        String farmerId,
        String name,
        Double areaHectares,
        String oliveVariety,
        GeoPointDto location,
        MaturityStatus maturityStatus,
        boolean publicVisibility,
        Instant createdAt,
        Instant updatedAt) {

    public static OrchardResponse from(OrchardDocument o) {
        return new OrchardResponse(
                o.getId(),
                o.getFarmerId(),
                o.getName(),
                o.getAreaHectares(),
                o.getOliveVariety(),
                GeoPointDto.from(o.getLocation()),
                o.getMaturityStatus(),
                o.isPublicVisibility(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
