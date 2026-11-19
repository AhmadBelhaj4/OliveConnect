package tn.oliveconnect.importer.service;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.oliveconnect.common.enums.ImportType;

/** Convention de nommage : <IMPORT_TYPE>_*.csv ou .json (ex. orchards_20261125.csv, presses_sfax.json). */
@Component
public class ScheduledInboxImporter {

    private static final Logger log = LoggerFactory.getLogger(ScheduledInboxImporter.class);
    private final ImportService importService;
    private final Path inbox;
    private final Path processed;

    public ScheduledInboxImporter(ImportService importService,
                                  @Value("${oliveconnect.import.inbox-dir:../sample-data/inbox}") String inboxDir,
                                  @Value("${oliveconnect.import.processed-dir:../sample-data/processed}") String processedDir) {
        this.importService = importService;
        this.inbox = Path.of(inboxDir);
        this.processed = Path.of(processedDir);
    }

    @Scheduled(fixedDelayString = "${oliveconnect.import.poll-ms:60000}")
    public void pollInbox() {
        if (!Files.isDirectory(inbox)) return;
        try (var stream = Files.list(inbox)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                ImportType type = detectType(file.getFileName().toString());
                if (type == null) continue;
                try (InputStream in = Files.newInputStream(file)) {
                    importService.runImport(type, file.getFileName().toString(), in);
                    Files.createDirectories(processed);
                    Files.move(file, processed.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    log.info("Import planifié terminé pour {}", file.getFileName());
                } catch (Exception e) {
                    log.error("Échec import planifié pour {}", file.getFileName(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Erreur lecture dossier inbox {}", inbox, e);
        }
    }

    private ImportType detectType(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        for (ImportType t : ImportType.values()) {
            if (upper.startsWith(t.name())) return t;
        }
        return null;
    }
}
