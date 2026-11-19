package tn.oliveconnect.data;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;
import tn.oliveconnect.orchard.document.OrchardDocument;
import tn.oliveconnect.user.document.UserDocument;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        try {
            // Index unique sur email
            mongoTemplate.indexOps(UserDocument.class)
                    .ensureIndex(new Index().on("email", Sort.Direction.ASC).unique());

            // Index 2dsphere sur la localisation du verger
            mongoTemplate.indexOps(OrchardDocument.class)
                    .ensureIndex(new GeospatialIndex("location"));

            // Index sur farmerId pour les vergers
            mongoTemplate.indexOps(OrchardDocument.class)
                    .ensureIndex(new Index().on("farmerId", Sort.Direction.ASC));

            // Index sur farmerId et status pour les demandes de récolte
            mongoTemplate.indexOps(HarvestRequestDocument.class)
                    .ensureIndex(new Index().on("farmerId", Sort.Direction.ASC));
            mongoTemplate.indexOps(HarvestRequestDocument.class)
                    .ensureIndex(new Index().on("status", Sort.Direction.ASC));

            log.info("Index MongoDB initialisés avec succès.");
        } catch (Exception e) {
            log.warn("Initialisation des index MongoDB différée ou conteneur non joignable lors du scan initial : {}", e.getMessage());
        }
    }
}
