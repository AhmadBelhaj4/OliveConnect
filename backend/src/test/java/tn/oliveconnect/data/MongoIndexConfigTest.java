package tn.oliveconnect.data;

import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tn.oliveconnect.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MongoIndexConfigTest extends AbstractIntegrationTest {

    @Test
    void allVersionedIndexesExist() {
        assertThat(indexNames("users")).contains("idx_users_email_uq");
        assertThat(indexNames("orchards")).contains("idx_orchards_location_2dsphere", "idx_orchards_farmer");
        assertThat(indexNames("harvest_requests")).contains(
                "idx_hr_farmer_created", "idx_hr_status_prefdate", "idx_hr_orchard_status");
        assertThat(indexNames("field_alerts")).contains("idx_alerts_status_prio_created");
        assertThat(indexNames("tours")).contains("idx_tours_date_status", "idx_tours_leader_date");
        assertThat(indexNames("maturity_readings")).contains("idx_maturity_orchard_obs");
        assertThat(indexNames("press_receptions")).contains("idx_pr_tour_bin_uq", "idx_pr_received_at");
    }

    @Test
    void geo2dsphereIndex_allowsNearSphereQuery() {
        Document sfaxOrchard = new Document("name", "Sfax")
                .append("location", new Document("type", "Point").append("coordinates", List.of(10.76, 34.73)));
        mongo.getCollection("orchards").insertOne(sfaxOrchard);

        // nearSphere exige un index 2dsphere actif : échoue si l'index n'est pas créé
        Query near = new Query(Criteria.where("location").nearSphere(new Point(10.76, 34.73)).maxDistance(0.01));
        List<Document> found = mongo.find(near, Document.class, "orchards");
        assertThat(found).hasSize(1);
    }

    @Test
    void uniqueIndexes_rejectDuplicates() {
        mongo.insert(new Document("email", "dup@example.test"), "users");
        assertThatThrownBy(() ->
                mongo.insert(new Document("email", "dup@example.test"), "users"))
                .isInstanceOf(DuplicateKeyException.class);

        mongo.insert(new Document("tourId", "t1").append("binId", "b1"), "press_receptions");
        assertThatThrownBy(() ->
                mongo.insert(new Document("tourId", "t1").append("binId", "b1"), "press_receptions"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private List<String> indexNames(String collection) {
        return mongo.indexOps(collection).getIndexInfo().stream().map(IndexInfo::getName).toList();
    }
}
