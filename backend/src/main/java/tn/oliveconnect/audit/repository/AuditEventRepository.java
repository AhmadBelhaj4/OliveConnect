package tn.oliveconnect.audit.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.audit.document.AuditEventDocument;

public interface AuditEventRepository extends MongoRepository<AuditEventDocument, String> {}
