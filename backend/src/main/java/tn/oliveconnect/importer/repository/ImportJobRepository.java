package tn.oliveconnect.importer.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.importer.document.ImportJobDocument;

public interface ImportJobRepository extends MongoRepository<ImportJobDocument, String> {}
