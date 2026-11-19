package tn.oliveconnect.resource.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.resource.document.BinDocument;

public interface BinRepository extends MongoRepository<BinDocument, String> {
    Page<BinDocument> findByStatus(ResourceStatus status, Pageable pageable);
}
