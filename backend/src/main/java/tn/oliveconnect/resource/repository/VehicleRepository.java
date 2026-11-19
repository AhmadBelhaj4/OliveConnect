package tn.oliveconnect.resource.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.resource.document.VehicleDocument;

public interface VehicleRepository extends MongoRepository<VehicleDocument, String> {
    Page<VehicleDocument> findByStatus(ResourceStatus status, Pageable pageable);
}
