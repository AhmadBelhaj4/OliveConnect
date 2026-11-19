package tn.oliveconnect.resource.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.common.enums.EquipmentType;
import tn.oliveconnect.common.enums.ResourceStatus;
import tn.oliveconnect.resource.document.EquipmentDocument;

import java.util.List;

public interface EquipmentRepository extends MongoRepository<EquipmentDocument, String> {
    List<EquipmentDocument> findByType(EquipmentType type);
    Page<EquipmentDocument> findByStatus(ResourceStatus status, Pageable pageable);
}
