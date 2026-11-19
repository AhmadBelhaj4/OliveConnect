package tn.oliveconnect.press.document;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import tn.oliveconnect.common.enums.QualityGrade;

@Document("press_receptions")
@Getter @Setter @NoArgsConstructor
public class PressReceptionDocument {
    @Id private String id;
    private String tourId;
    private String binId;
    private String pressId;
    private Instant receivedAt;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal weightKg;
    private QualityGrade qualityGrade;
    private String notes;
    private String recordedBy;
}
