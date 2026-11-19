package tn.oliveconnect.harvest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;

public interface HarvestRequestRepository extends MongoRepository<HarvestRequestDocument, String> {
    Page<HarvestRequestDocument> findByFarmerId(String farmerId, Pageable pageable);
}
