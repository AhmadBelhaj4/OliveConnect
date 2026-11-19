package tn.oliveconnect.orchard.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.oliveconnect.orchard.document.OrchardDocument;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrchardRepository extends MongoRepository<OrchardDocument, String> {
    List<OrchardDocument> findByFarmerId(String farmerId);
    Page<OrchardDocument> findByFarmerId(String farmerId, Pageable pageable);
    Optional<OrchardDocument> findByIdAndFarmerId(String id, String farmerId);
    List<OrchardDocument> findByLocationNear(Point point, Distance distance);
}
