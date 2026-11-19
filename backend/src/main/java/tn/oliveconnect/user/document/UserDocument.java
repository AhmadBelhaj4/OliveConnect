package tn.oliveconnect.user.document;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.oliveconnect.common.enums.Role;

@Document("users")
@Getter @Setter @NoArgsConstructor
public class UserDocument {
    @Id private String id;
    private String fullName;
    private String email;
    private String passwordHash;
    private String phone;
    private Role role;
    private boolean active;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
