package tn.oliveconnect.map.dto;

import java.util.List;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.enums.MaturityStatus;
import tn.oliveconnect.common.web.GeoPointDto;

public final class MapDtos {
    private MapDtos() {}

    public record OrchardMapProperties(
            String orchardId, String name, String oliveVariety, Double areaHectares,
            String farmerId, MaturityStatus maturityStatus, HarvestStatus harvestStatus) {}

    public record OrchardMapFeature(String type, GeoPointDto geometry, OrchardMapProperties properties) {}

    public record OrchardMapFeatureCollection(String type, boolean truncated, List<OrchardMapFeature> features) {}
}
