package tn.oliveconnect.harvest.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;

/** Filtres dynamiques de la file du responsable (DAO : uniquement de la requête). */
@Repository
public class HarvestRequestSearchRepository {
    private final MongoTemplate mongo;
    public HarvestRequestSearchRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public Page<HarvestRequestDocument> search(HarvestStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        Query q = new Query();
        if (status != null) q.addCriteria(Criteria.where("status").is(status));
        if (from != null || to != null) {
            Criteria c = Criteria.where("preferredDate");
            if (from != null) c = c.gte(from);
            if (to != null) c = c.lte(to);
            q.addCriteria(c);
        }
        long total = mongo.count(q, HarvestRequestDocument.class);
        List<HarvestRequestDocument> content = mongo.find(Query.of(q).with(pageable), HarvestRequestDocument.class);
        return new PageImpl<>(content, pageable, total);
    }
}
