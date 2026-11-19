package tn.oliveconnect.press.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.press.document.PressDocument;

public interface PressRepository extends MongoRepository<PressDocument, String> {}
