package tn.oliveconnect.map.service;

import java.time.Duration;
import java.util.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import tn.oliveconnect.cache.CacheService;
import tn.oliveconnect.common.enums.*;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.web.GeoPointDto;
import tn.oliveconnect.map.dto.MapDtos.*;
import tn.oliveconnect.security.AuthUser;

@Service
public class OrchardMapService {

    private static final int MAX_RESULTS = 500;
    private static final List<String> ACTIVE_STATUSES = List.of("SUBMITTED", "APPROVED", "SCHEDULED", "IN_PROGRESS");

    private final MongoTemplate mongo;
    private final CacheService cache;

    public OrchardMapService(MongoTemplate mongo, CacheService cache) {
        this.mongo = mongo;
        this.cache = cache;
    }

    public OrchardMapFeatureCollection findInBbox(AuthUser viewer, String bboxParam,
                                                  MaturityStatus maturity, String variety,
                                                  HarvestStatus harvestStatusFilter) {
        boolean isStaff = viewer != null && viewer.isStaff();
        boolean isFarmer = viewer != null && viewer.hasRole(Role.FARMER);
        String scope = isStaff ? "STAFF" : (isFarmer ? "FARMER_" + viewer.id() : "PUBLIC");
        String filterHash = CacheService.sha256(bboxParam + "|" + maturity + "|" + variety + "|" + harvestStatusFilter);
        String cacheKey = "oliveconnect:map:" + scope + ":" + filterHash;

        return cache.getOrLoad(cacheKey, Duration.ofSeconds(45), OrchardMapFeatureCollection.class,
                () -> queryMongoMap(viewer, bboxParam, maturity, variety, harvestStatusFilter));
    }

    private OrchardMapFeatureCollection queryMongoMap(AuthUser viewer, String bboxParam,
                                                     MaturityStatus maturity, String variety,
                                                     HarvestStatus harvestStatusFilter) {
        double[] b = parseBbox(bboxParam);   // [minLon, minLat, maxLon, maxLat]
        Box box = new Box(new Point(b[0], b[1]), new Point(b[2], b[3]));

        Query q = new Query();
        q.addCriteria(Criteria.where("location").within(box));

        // Périmètre selon le rôle (§5.5 Carte) :
        boolean isStaff = viewer != null && viewer.isStaff();
        boolean isFarmer = viewer != null && viewer.hasRole(Role.FARMER);
        if (!isStaff) {
            if (isFarmer) {
                q.addCriteria(new Criteria().orOperator(
                        Criteria.where("publicVisibility").is(true),
                        Criteria.where("farmerId").is(new ObjectId(viewer.id()))));
            } else {
                q.addCriteria(Criteria.where("publicVisibility").is(true));
            }
        }
        if (maturity != null) q.addCriteria(Criteria.where("maturityStatus").is(maturity.name()));
        if (variety != null && !variety.isBlank()) q.addCriteria(Criteria.where("oliveVariety").is(variety.trim()));

        // On demande MAX_RESULTS + 1 pour savoir si truncated = true
        q.limit(MAX_RESULTS + 1);
        List<Document> docs = mongo.find(q, Document.class, "orchards");

        // Récupérer le statut de demande active par verger en 1 requête groupée
        List<ObjectId> orchardIds = docs.stream().map(d -> d.getObjectId("_id")).toList();
        Map<String, HarvestStatus> activeByOrchard = loadActiveHarvestStatuses(orchardIds);

        List<OrchardMapFeature> features = new ArrayList<>();
        boolean truncated = false;

        for (Document doc : docs) {
            String orchardId = doc.getObjectId("_id").toHexString();
            HarvestStatus activeStatus = activeByOrchard.get(orchardId);
            if (harvestStatusFilter != null && activeStatus != harvestStatusFilter) {
                continue;
            }
            if (features.size() == MAX_RESULTS) {
                truncated = true;
                break;
            }
            String farmerId = doc.getObjectId("farmerId") != null ? doc.getObjectId("farmerId").toHexString() : null;
            boolean canSeePrivate = isStaff || (isFarmer && viewer.id().equals(farmerId));

            Document loc = doc.get("location", Document.class);
            @SuppressWarnings("unchecked")
            List<Number> coords = (List<Number>) loc.get("coordinates");
            GeoPointDto point = new GeoPointDto("Point", List.of(coords.get(0).doubleValue(), coords.get(1).doubleValue()));

            MaturityStatus mat = doc.getString("maturityStatus") != null
                    ? MaturityStatus.valueOf(doc.getString("maturityStatus")) : MaturityStatus.UNKNOWN;

            features.add(new OrchardMapFeature("Feature", point, new OrchardMapProperties(
                    orchardId,
                    canSeePrivate ? doc.getString("name") : null,
                    doc.getString("oliveVariety"),
                    canSeePrivate ? (doc.get("areaHectares") instanceof Number n ? n.doubleValue() : null) : null,
                    canSeePrivate ? farmerId : null,
                    mat,
                    activeStatus)));
        }
        return new OrchardMapFeatureCollection("FeatureCollection", truncated, features);
    }

    private Map<String, HarvestStatus> loadActiveHarvestStatuses(List<ObjectId> orchardIds) {
        if (orchardIds.isEmpty()) return Map.of();
        Query q = new Query(Criteria.where("orchardId").in(orchardIds).and("status").in(ACTIVE_STATUSES));
        List<Document> reqs = mongo.find(q, Document.class, "harvest_requests");
        Map<String, HarvestStatus> map = new HashMap<>();
        for (Document r : reqs) {
            String oid = r.getObjectId("orchardId").toHexString();
            map.putIfAbsent(oid, HarvestStatus.valueOf(r.getString("status")));
        }
        return map;
    }

    private double[] parseBbox(String raw) {
        if (raw == null || raw.isBlank()) throw ApiException.validation("bbox", "Paramètre bbox obligatoire");
        String[] parts = raw.split(",");
        if (parts.length != 4) throw ApiException.validation("bbox", "Format attendu : minLon,minLat,maxLon,maxLat");
        try {
            double minLon = Double.parseDouble(parts[0].trim()), minLat = Double.parseDouble(parts[1].trim()),
                   maxLon = Double.parseDouble(parts[2].trim()), maxLat = Double.parseDouble(parts[3].trim());
            if (minLon < -180 || maxLon > 180 || minLat < -90 || maxLat > 90 || minLon >= maxLon || minLat >= maxLat) {
                throw ApiException.validation("bbox", "Bornes géographiques invalides");
            }
            return new double[]{minLon, minLat, maxLon, maxLat};
        } catch (NumberFormatException e) {
            throw ApiException.validation("bbox", "Coordonnées numériques invalides");
        }
    }
}
