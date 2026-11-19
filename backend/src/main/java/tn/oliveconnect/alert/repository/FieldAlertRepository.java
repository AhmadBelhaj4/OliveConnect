package tn.oliveconnect.alert.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.alert.document.FieldAlertDocument;

public interface FieldAlertRepository extends MongoRepository<FieldAlertDocument, String> {}
