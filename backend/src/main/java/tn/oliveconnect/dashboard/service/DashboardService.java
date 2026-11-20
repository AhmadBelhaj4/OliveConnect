package tn.oliveconnect.dashboard.service;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import tn.oliveconnect.cache.CacheService;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.dashboard.dto.DashboardDtos.*;

@Service
public class DashboardService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(90); // §3.3 : 90 s
    private final MongoTemplate mongo;
    private final CacheService cache;

    public DashboardService(MongoTemplate mongo, CacheService cache) {
        this.mongo = mongo;
        this.cache = cache;
    }

    public DashboardSummaryResponse summary(LocalDate from, LocalDate to) {
        validateRange(from, to);
        String key = "oliveconnect:dashboard:summary:" + from + "_" + to;
        return cache.getOrLoad(key, CACHE_TTL, DashboardSummaryResponse.class, () -> computeSummary(from, to));
    }

    public YieldByOrchardResponse yieldByOrchard(LocalDate from, LocalDate to) {
        validateRange(from, to);
        String key = "oliveconnect:dashboard:yield:" + from + "_" + to;
        return cache.getOrLoad(key, CACHE_TTL, YieldByOrchardResponse.class, () -> computeYield(from, to));
    }

    public CollectionDelaysResponse collectionDelays(LocalDate from, LocalDate to) {
        validateRange(from, to);
        String key = "oliveconnect:dashboard:delays:" + from + "_" + to;
        return cache.getOrLoad(key, CACHE_TTL, CollectionDelaysResponse.class, () -> computeDelays(from, to));
    }

    public BinFillRatesResponse binFillRates(LocalDate from, LocalDate to) {
        validateRange(from, to);
        String key = "oliveconnect:dashboard:bins:" + from + "_" + to;
        return cache.getOrLoad(key, CACHE_TTL, BinFillRatesResponse.class, () -> computeBinFillRates(from, to));
    }

    // --- 1. SUMMARY ---
    private DashboardSummaryResponse computeSummary(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Total récolté (demandes COMPLETED sur la période)
        Aggregation harvestedAgg = newAggregation(
                match(Criteria.where("status").is("COMPLETED").and("completedAt").gte(start).lt(end)),
                group().sum(ConvertOperators.ToDouble.toDouble("$actualQuantityKg")).as("total"));
        Document hDoc = mongo.aggregate(harvestedAgg, "harvest_requests", Document.class).getUniqueMappedResult();
        double totalHarvested = hDoc != null && hDoc.get("total") instanceof Number n ? round1(n.doubleValue()) : 0.0;

        // Total reçu au pressoir
        Aggregation receivedAgg = newAggregation(
                match(Criteria.where("receivedAt").gte(start).lt(end)),
                group().sum(ConvertOperators.ToDouble.toDouble("$weightKg")).as("total"));
        Document rDoc = mongo.aggregate(receivedAgg, "press_receptions", Document.class).getUniqueMappedResult();
        double totalReceived = rDoc != null && rDoc.get("total") instanceof Number n ? round1(n.doubleValue()) : 0.0;

        long completedTours = mongo.count(Query.query(
                Criteria.where("status").is("COMPLETED").and("completedAt").gte(start).lt(end)), "tours");
        long openAlerts = mongo.count(Query.query(Criteria.where("status").is("OPEN")), "field_alerts");

        // Répartition des demandes par statut : TOUTES les 7 clés présentes, à 0 si besoin (§5.5)
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (HarvestStatus st : HarvestStatus.values()) byStatus.put(st.name(), 0L);
        Aggregation statusAgg = newAggregation(
                match(Criteria.where("requestedAt").gte(start).lt(end)),
                group("status").count().as("count"));
        for (Document d : mongo.aggregate(statusAgg, "harvest_requests", Document.class)) {
            if (d.getString("_id") != null) byStatus.put(d.getString("_id"), ((Number) d.get("count")).longValue());
        }
        return new DashboardSummaryResponse(from, to, totalHarvested, totalReceived, completedTours, openAlerts, byStatus);
    }

    // --- 2. YIELD BY ORCHARD ---
    private YieldByOrchardResponse computeYield(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Aggregation agg = newAggregation(
                match(Criteria.where("status").is("COMPLETED").and("completedAt").gte(start).lt(end)),
                group("orchardId")
                        .first("orchardName").as("orchardName")
                        .sum(ConvertOperators.ToDouble.toDouble("$actualQuantityKg")).as("totalHarvestedKg")
                        .count().as("collectionCount"),
                sort(Sort.Direction.DESC, "totalHarvestedKg"));

        List<OrchardYieldItem> items = mongo.aggregate(agg, "harvest_requests", Document.class)
                .getMappedResults().stream()
                .map(d -> new OrchardYieldItem(
                        String.valueOf(d.get("_id")),
                        d.getString("orchardName"),
                        round1(((Number) d.get("totalHarvestedKg")).doubleValue()),
                        ((Number) d.get("collectionCount")).longValue()))
                .toList();
        return new YieldByOrchardResponse(from, to, items);
    }

    // --- 3. COLLECTION DELAYS ---
    private CollectionDelaysResponse computeDelays(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Aggregation agg = newAggregation(
                match(Criteria.where("status").is("COMPLETED")
                        .and("completedAt").gte(start).lt(end)
                        .and("requestedAt").ne(null).and("scheduledAt").ne(null)),
                project()
                        .and(ArithmeticOperators.Divide.valueOf(
                                ArithmeticOperators.Subtract.valueOf("scheduledAt").subtract("requestedAt"))
                                .divideBy(3_600_000.0)).as("subToSched")
                        .and(ArithmeticOperators.Divide.valueOf(
                                ArithmeticOperators.Subtract.valueOf("completedAt").subtract("scheduledAt"))
                                .divideBy(3_600_000.0)).as("schedToComp"),
                group().count().as("sampleSize")
                        .avg("subToSched").as("avgSubToSched")
                        .avg("schedToComp").as("avgSchedToComp"));

        Document d = mongo.aggregate(agg, "harvest_requests", Document.class).getUniqueMappedResult();
        if (d == null) return new CollectionDelaysResponse(from, to, 0, null, null);
        return new CollectionDelaysResponse(from, to,
                ((Number) d.get("sampleSize")).longValue(),
                round1(((Number) d.get("avgSubToSched")).doubleValue()),
                round1(((Number) d.get("avgSchedToComp")).doubleValue()));
    }

    // --- 4. BIN FILL RATES ---
    private BinFillRatesResponse computeBinFillRates(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Aggregation agg = newAggregation(
                match(Criteria.where("receivedAt").gte(start).lt(end)),
                project("binId").and(ConvertOperators.ToDouble.toDouble("$weightKg")).as("weightKg")
                        .and(ConvertOperators.ToObjectId.toObjectId("$binId")).as("binObjId"),
                lookup("bins", "binObjId", "_id", "bin"),
                unwind("bin"),
                project("binId", "weightKg")
                        .and("bin.code").as("binCode")
                        .and("bin.capacityKg").as("capacityKg")
                        .and(ArithmeticOperators.Divide.valueOf("weightKg").divideBy("bin.capacityKg")).as("fillRate"),
                group("binId")
                        .first("binCode").as("binCode")
                        .first("capacityKg").as("capacityKg")
                        .count().as("receptionCount")
                        .avg("fillRate").as("avgFillRate"),
                sort(Sort.Direction.DESC, "avgFillRate"));

        List<BinFillRateItem> items = mongo.aggregate(agg, "press_receptions", Document.class)
                .getMappedResults().stream()
                .map(d -> new BinFillRateItem(
                        d.getString("_id"),
                        d.getString("binCode"),
                        ((Number) d.get("capacityKg")).intValue(),
                        ((Number) d.get("receptionCount")).longValue(),
                        Math.round(((Number) d.get("avgFillRate")).doubleValue() * 100.0) / 100.0))
                .toList();
        return new BinFillRatesResponse(from, to, items);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw ApiException.validation("from", "from et to obligatoires");
        if (from.isAfter(to)) throw ApiException.validation("from", "from doit être <= to");
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw ApiException.validation("to", "Période maximale autorisée : 366 jours");
        }
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
