package tn.oliveconnect.user.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.user.document.UserDocument;

public interface UserRepository extends MongoRepository<UserDocument, String> {
    Optional<UserDocument> findByEmail(String email);
    boolean existsByEmail(String email);
}
