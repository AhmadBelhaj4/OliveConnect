package tn.oliveconnect.resource.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.resource.document.WorkerDocument;

public interface WorkerRepository extends MongoRepository<WorkerDocument, String> {
    Page<WorkerDocument> findByStatus(ResourceStatus status, Pageable pageable);
}
