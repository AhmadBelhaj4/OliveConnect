package tn.oliveconnect.data;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Crée de façon idempotente tous les index décrits dans docs/nosql-model.md (tâche B3).
 */
@Configuration
public class MongoIndexConfig {

    private final MongoTemplate mongo;

    public MongoIndexConfig(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @PostConstruct
    public void ensureIndexes() {
        // Drop any legacy unversioned indexes (e.g. created by Spring Data @Indexed before auto-index-creation was disabled)
        for (String col : List.of("users", "orchards", "harvest_requests", "field_alerts", "tours", "maturity_readings", "press_receptions")) {
            try {
                mongo.indexOps(col).getIndexInfo().forEach(info -> {
                    if (!"_id_".equals(info.getName()) && !info.getName().startsWith("idx_")) {
                        try { mongo.indexOps(col).dropIndex(info.getName()); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}
        }

        // 1. users : email unique
        mongo.indexOps("users").ensureIndex(
                new Index().on("email", Sort.Direction.ASC).unique().named("idx_users_email_uq"));

        // 2. orchards : 2dsphere sur location + index farmerId
        mongo.indexOps("orchards").ensureIndex(
                new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE)
                        .named("idx_orchards_location_2dsphere"));
        mongo.indexOps("orchards").ensureIndex(
                new Index().on("farmerId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
                        .named("idx_orchards_farmer"));

        // 3. harvest_requests : 3 index (mes demandes, file responsable, statut par verger)
        mongo.indexOps("harvest_requests").ensureIndex(
                new Index().on("farmerId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
                        .named("idx_hr_farmer_created"));
        mongo.indexOps("harvest_requests").ensureIndex(
                new CompoundIndexDefinition(new Document("status", 1).append("preferredDate", 1).append("createdAt", -1))
                        .named("idx_hr_status_prefdate"));
        mongo.indexOps("harvest_requests").ensureIndex(
                new Index().on("orchardId", Sort.Direction.ASC).on("status", Sort.Direction.ASC)
                        .named("idx_hr_orchard_status"));

        // 4. field_alerts : {status, priority, createdAt}
        mongo.indexOps("field_alerts").ensureIndex(
                new CompoundIndexDefinition(new Document("status", 1).append("priority", 1).append("createdAt", -1))
                        .named("idx_alerts_status_prio_created"));

        // 5. tours : 2 index (date + statut pour conflits ; chef d'équipe + date)
        mongo.indexOps("tours").ensureIndex(
                new Index().on("tourDate", Sort.Direction.ASC).on("status", Sort.Direction.ASC)
                        .named("idx_tours_date_status"));
        mongo.indexOps("tours").ensureIndex(
                new Index().on("teamLeaderId", Sort.Direction.ASC).on("tourDate", Sort.Direction.DESC)
                        .named("idx_tours_leader_date"));

        // 6. maturity_readings
        mongo.indexOps("maturity_readings").ensureIndex(
                new Index().on("orchardId", Sort.Direction.ASC).on("observedAt", Sort.Direction.DESC)
                        .named("idx_maturity_orchard_obs"));

        // 7. press_receptions : couple (tourId, binId) unique + date de réception
        mongo.indexOps("press_receptions").ensureIndex(
                new CompoundIndexDefinition(new Document("tourId", 1).append("binId", 1))
                        .unique().named("idx_pr_tour_bin_uq"));
        mongo.indexOps("press_receptions").ensureIndex(
                new Index().on("receivedAt", Sort.Direction.DESC).named("idx_pr_received_at"));

        // 8. externalId sparse unique indexes pour l'import idempotent (A15)
        for (String col : List.of("orchards", "workers", "vehicles", "presses", "maturity_readings", "weather_observations")) {
            try {
                mongo.indexOps(col).ensureIndex(
                        new Index().on("externalId", Sort.Direction.ASC).unique().sparse()
                                .named("idx_" + col + "_external_id_uq"));
            } catch (Exception ignored) {}
        }
    }
}
