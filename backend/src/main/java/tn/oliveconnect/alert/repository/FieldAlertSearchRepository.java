package tn.oliveconnect.alert.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import tn.oliveconnect.alert.document.FieldAlertDocument;
import tn.oliveconnect.common.enums.AlertPriority;
import tn.oliveconnect.common.enums.AlertStatus;

@Repository
public class FieldAlertSearchRepository {
    private final MongoTemplate mongo;
    public FieldAlertSearchRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public Page<FieldAlertDocument> search(String farmerIdOrNull, AlertStatus status,
                                           AlertPriority priority, Pageable pageable) {
        Query q = new Query();
        if (farmerIdOrNull != null) q.addCriteria(Criteria.where("farmerId").is(farmerIdOrNull));
        if (status != null) q.addCriteria(Criteria.where("status").is(status));
        if (priority != null) q.addCriteria(Criteria.where("priority").is(priority));
        long total = mongo.count(q, FieldAlertDocument.class);
        List<FieldAlertDocument> content = mongo.find(Query.of(q).with(pageable), FieldAlertDocument.class);
        return new PageImpl<>(content, pageable, total);
    }
}
