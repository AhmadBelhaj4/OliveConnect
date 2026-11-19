package tn.oliveconnect.user.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.oliveconnect.user.document.Role;
import tn.oliveconnect.user.document.UserDocument;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<UserDocument, String> {
    Optional<UserDocument> findByEmail(String email);
    boolean existsByEmail(String email);
    List<UserDocument> findByRole(Role role);
}
