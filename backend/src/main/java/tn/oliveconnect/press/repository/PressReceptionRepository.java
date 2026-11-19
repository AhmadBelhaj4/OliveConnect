package tn.oliveconnect.press.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.press.document.PressReceptionDocument;

public interface PressReceptionRepository extends MongoRepository<PressReceptionDocument, String> {
    boolean existsByTourIdAndBinId(String tourId, String binId);
}
