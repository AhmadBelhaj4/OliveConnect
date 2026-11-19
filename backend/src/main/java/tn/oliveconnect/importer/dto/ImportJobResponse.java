package tn.oliveconnect.importer.dto;

import java.time.Instant;
import java.util.List;
import tn.oliveconnect.common.enums.ImportStatus;
import tn.oliveconnect.common.enums.ImportType;
import tn.oliveconnect.importer.document.ImportJobDocument;

public record ImportJobResponse(
        String id, ImportType type, String sourceFile, ImportStatus status,
        Instant startedAt, Instant finishedAt, int readCount, int insertedCount,
        int updatedCount, int rejectedCount, List<ImportJobDocument.RowError> errors) {

    public static ImportJobResponse from(ImportJobDocument j) {
        return new ImportJobResponse(j.getId(), j.getType(), j.getSourceFile(), j.getStatus(),
                j.getStartedAt(), j.getFinishedAt(), j.getReadCount(), j.getInsertedCount(),
                j.getUpdatedCount(), j.getRejectedCount(), j.getErrors());
    }
}
