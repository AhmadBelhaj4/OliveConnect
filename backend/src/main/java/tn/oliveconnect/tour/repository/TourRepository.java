package tn.oliveconnect.tour.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.common.enums.TourStatus;
import tn.oliveconnect.tour.document.TourDocument;

public interface TourRepository extends MongoRepository<TourDocument, String> {
    List<TourDocument> findByTourDateAndStatusNot(LocalDate tourDate, TourStatus excludedStatus);
    Page<TourDocument> findByTeamLeaderId(String teamLeaderId, Pageable pageable);
    Optional<TourDocument> findByStopsHarvestRequestIdAndStatusNot(String harvestRequestId, TourStatus excludedStatus);
}
