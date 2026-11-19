package tn.oliveconnect.harvest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;
import tn.oliveconnect.harvest.document.HarvestStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface HarvestRequestRepository extends MongoRepository<HarvestRequestDocument, String> {
    List<HarvestRequestDocument> findByFarmerId(String farmerId);
    Page<HarvestRequestDocument> findByFarmerId(String farmerId, Pageable pageable);
    Optional<HarvestRequestDocument> findByIdAndFarmerId(String id, String farmerId);
    List<HarvestRequestDocument> findByStatus(HarvestStatus status);
    List<HarvestRequestDocument> findByOrchardId(String orchardId);
}
