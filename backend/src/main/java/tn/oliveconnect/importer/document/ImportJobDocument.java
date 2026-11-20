package tn.oliveconnect.importer.document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.oliveconnect.common.enums.ImportStatus;
import tn.oliveconnect.common.enums.ImportType;

@Document("import_jobs")
@Getter @Setter @NoArgsConstructor
public class ImportJobDocument {
    @Id private String id;
    private ImportType type;
    private String sourceFile;
    private ImportStatus status;
    private Instant startedAt;
    private Instant finishedAt;
    private int readCount;
    private int insertedCount;
    private int updatedCount;
    private int rejectedCount;
    private List<RowError> errors = new ArrayList<>();   // 100 premières erreurs max (§5.5)

    public record RowError(int row, String field, String message) {}
}
