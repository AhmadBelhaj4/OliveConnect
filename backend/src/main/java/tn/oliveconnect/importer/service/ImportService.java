package tn.oliveconnect.importer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.oliveconnect.common.enums.*;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.facade.CacheInvalidator;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.importer.document.ImportJobDocument;
import tn.oliveconnect.importer.document.ImportJobDocument.RowError;
import tn.oliveconnect.importer.dto.ImportJobResponse;
import tn.oliveconnect.importer.repository.ImportJobRepository;
import tn.oliveconnect.user.repository.UserRepository;

@Service
public class ImportService {

    private static final int MAX_ERRORS = 100;
    private final ImportJobRepository jobs;
    private final UserRepository users;
    private final MongoTemplate mongo;
    private final ObjectMapper mapper;
    private final CacheInvalidator cache;
    private final long maxBytes;

    public ImportService(ImportJobRepository jobs, UserRepository users, MongoTemplate mongo,
                         ObjectMapper mapper, CacheInvalidator cache,
                         @Value("${oliveconnect.import.max-bytes:5242880}") long maxBytes) {
        this.jobs = jobs;
        this.users = users;
        this.mongo = mongo;
        this.mapper = mapper;
        this.cache = cache;
        this.maxBytes = maxBytes;
    }

    public ImportJobResponse importMultipart(ImportType type, MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.validation("file", "Fichier obligatoire");
        if (file.getSize() > maxBytes) throw ApiException.validation("file", "Taille maximale dépassée (5 Mo)");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("upload.csv");
        try {
            return runImport(type, name, file.getInputStream());
        } catch (IOException e) {
            throw ApiException.validation("file", "Impossible de lire le fichier");
        }
    }

    public ImportJobResponse runImport(ImportType type, String fileName, InputStream in) throws IOException {
        boolean isJson = fileName.toLowerCase(Locale.ROOT).endsWith(".json");
        boolean isCsv = fileName.toLowerCase(Locale.ROOT).endsWith(".csv");
        if (!isJson && !isCsv) throw ApiException.validation("file", "Extension attendue : .csv ou .json");

        ImportJobDocument job = new ImportJobDocument();
        job.setType(type);
        job.setSourceFile(fileName);
        job.setStatus(ImportStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job = jobs.save(job);

        List<Map<String, String>> rows = isJson ? parseJson(in) : parseCsv(in);
        int inserted = 0;
        int updated = 0;
        int rejected = 0;
        List<RowError> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2; // ligne 1 = en-tête CSV
            Map<String, String> row = rows.get(i);
            try {
                boolean wasInserted = upsertRow(type, row);
                if (wasInserted) inserted++; else updated++;
            } catch (RowValidationException ex) {
                rejected++;
                if (errors.size() < MAX_ERRORS) errors.add(new RowError(rowNum, ex.field, ex.getMessage()));
            } catch (Exception ex) {
                rejected++;
                if (errors.size() < MAX_ERRORS) errors.add(new RowError(rowNum, "row", ex.getMessage()));
            }
        }

        job.setReadCount(rows.size());
        job.setInsertedCount(inserted);
        job.setUpdatedCount(updated);
        job.setRejectedCount(rejected);
        job.setErrors(errors);
        job.setFinishedAt(Instant.now());
        if (rejected == 0) job.setStatus(ImportStatus.COMPLETED);
        else if (inserted + updated > 0) job.setStatus(ImportStatus.PARTIAL);
        else job.setStatus(ImportStatus.FAILED);
        job = jobs.save(job);

        // Invalidation Redis
        cache.mapChanged();
        cache.dashboardChanged();
        return ImportJobResponse.from(job);
    }

    public PageResponse<ImportJobResponse> list(Pageable pageable) {
        return PageResponse.from(jobs.findAll(pageable), ImportJobResponse::from);
    }

    public ImportJobResponse get(String id) {
        return jobs.findById(id).map(ImportJobResponse::from)
                .orElseThrow(() -> ApiException.notFound("Import introuvable."));
    }

    /** Upsert idempotent sur externalId : renvoie true si inséré, false si mis à jour. */
    private boolean upsertRow(ImportType type, Map<String, String> r) {
        String extId = requireText(r, "externalId");
        return switch (type) {
            case ORCHARDS -> {
                String email = requireText(r, "farmerEmail").toLowerCase(Locale.ROOT);
                var farmer = users.findByEmail(email)
                        .orElseThrow(() -> new RowValidationException("farmerEmail", "Agriculteur introuvable : " + email));
                String name = requireText(r, "name");
                double area = requirePositiveDouble(r, "areaHectares");
                String variety = requireText(r, "oliveVariety");
                double lon = requireRange(r, "longitude", -180, 180);
                double lat = requireRange(r, "latitude", -90, 90);
                boolean pub = Boolean.parseBoolean(r.getOrDefault("publicVisibility", "true"));
                Instant now = Instant.now();

                Update u = new Update()
                        .set("farmerId", new ObjectId(farmer.getId()))
                        .set("name", name)
                        .set("areaHectares", area)
                        .set("oliveVariety", variety)
                        .set("location", new Document("type", "Point").append("coordinates", List.of(lon, lat)))
                        .set("publicVisibility", pub)
                        .set("updatedAt", now)
                        .setOnInsert("maturityStatus", MaturityStatus.UNKNOWN.name())
                        .setOnInsert("createdAt", now);
                var res = mongo.upsert(Query.query(Criteria.where("externalId").is(extId)), u, "orchards");
                yield res.getUpsertedId() != null;
            }
            case WORKERS -> {
                String fullName = requireText(r, "fullName");
                String phone = requireText(r, "phone");
                List<String> skills = Arrays.stream(r.getOrDefault("skills", "PICKING").split("\\|"))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
                Update u = new Update().set("fullName", fullName).set("phone", phone).set("skills", skills)
                        .setOnInsert("status", ResourceStatus.AVAILABLE.name());
                yield mongo.upsert(Query.query(Criteria.where("externalId").is(extId)), u, "workers").getUpsertedId() != null;
            }
            case VEHICLES -> {
                VehicleType vt = requireEnum(r, "type", VehicleType.class);
                String reg = requireText(r, "registration");
                int cap = (int) requirePositiveDouble(r, "capacityKg");
                Update u = new Update().set("type", vt.name()).set("registration", reg).set("capacityKg", cap)
                        .setOnInsert("status", ResourceStatus.AVAILABLE.name());
                yield mongo.upsert(Query.query(Criteria.where("externalId").is(extId)), u, "vehicles").getUpsertedId() != null;
            }
            case PRESSES -> {
                String name = requireText(r, "name");
                double lon = requireRange(r, "longitude", -180, 180);
                double lat = requireRange(r, "latitude", -90, 90);
                int daily = (int) requirePositiveDouble(r, "dailyCapacityKg");
                int avail = (int) requireRange(r, "availableCapacityKg", 0, daily);
                Update u = new Update().set("name", name)
                        .set("location", new Document("type", "Point").append("coordinates", List.of(lon, lat)))
                        .set("dailyCapacityKg", daily).set("availableCapacityKg", avail);
                yield mongo.upsert(Query.query(Criteria.where("externalId").is(extId)), u, "presses").getUpsertedId() != null;
            }
            case MATURITY_READINGS -> {
                String orchardExt = requireText(r, "orchardExternalId");
                Document o = mongo.findOne(Query.query(Criteria.where("externalId").is(orchardExt)), Document.class, "orchards");
                if (o == null) throw new RowValidationException("orchardExternalId", "Verger introuvable");
                MaturityStatus ms = requireEnum(r, "maturityStatus", MaturityStatus.class);
                Instant obs = requireInstant(r, "observedAt");
                Update u = new Update().set("orchardId", o.getObjectId("_id")).set("maturityStatus", ms.name()).set("observedAt", obs);
                boolean ins = mongo.upsert(Query.query(Criteria.where("externalId").is(extId)), u, "maturity_readings").getUpsertedId() != null;
                mongo.updateFirst(Query.query(Criteria.where("_id").is(o.getObjectId("_id"))),
                        Update.update("maturityStatus", ms.name()), "orchards");
                cache.orchardChanged(o.getObjectId("_id").toHexString());
                yield ins;
            }
            case WEATHER_OBSERVATIONS -> {
                double lon = requireRange(r, "longitude", -180, 180);
                double lat = requireRange(r, "latitude", -90, 90);
                double temp = requireRange(r, "temperatureC", -30, 65);
                double rain = requireRange(r, "rainfallMm", 0, 1000);
                Instant obs = requireInstant(r, "observedAt");
                Update u = new Update()
                        .set("location", new Document("type", "Point").append("coordinates", List.of(lon, lat)))
                        .set("temperatureC", temp).set("rainfallMm", rain).set("observedAt", obs);
                yield mongo.upsert(Query.query(Criteria.where("externalId").is(extId)), u, "weather_observations").getUpsertedId() != null;
            }
            case PRESS_CAPACITY -> {
                String pressExt = requireText(r, "pressExternalId");
                Document p = mongo.findOne(Query.query(Criteria.where("externalId").is(pressExt)), Document.class, "presses");
                if (p == null) throw new RowValidationException("pressExternalId", "Pressoir introuvable");
                int daily = p.getInteger("dailyCapacityKg", 50000);
                int avail = (int) requireRange(r, "availableCapacityKg", 0, daily);
                mongo.updateFirst(Query.query(Criteria.where("_id").is(p.getObjectId("_id"))),
                        Update.update("availableCapacityKg", avail), "presses");
                yield false;
            }
        };
    }

    private List<Map<String, String>> parseCsv(InputStream in) throws IOException {
        List<String> lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                .lines().filter(l -> !l.isBlank()).toList();
        if (lines.size() < 2) return List.of();
        String[] headers = lines.get(0).split(",");
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] vals = lines.get(i).split(",", -1);
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < headers.length; c++) {
                row.put(headers[c].trim(), c < vals.length ? vals[c].trim() : "");
            }
            result.add(row);
        }
        return result;
    }

    private List<Map<String, String>> parseJson(InputStream in) throws IOException {
        List<Map<String, Object>> raw = mapper.readValue(in, new TypeReference<>() {});
        return raw.stream().map(m -> {
            Map<String, String> row = new LinkedHashMap<>();
            m.forEach((k, v) -> row.put(k, v == null ? "" : String.valueOf(v).trim()));
            return row;
        }).toList();
    }

    private String requireText(Map<String, String> r, String f) {
        String v = r.get(f);
        if (v == null || v.isBlank()) throw new RowValidationException(f, "Champ obligatoire");
        return v.trim();
    }
    private double requirePositiveDouble(Map<String, String> r, String f) {
        double v = requireRange(r, f, Double.MIN_VALUE, Double.MAX_VALUE);
        if (v <= 0) throw new RowValidationException(f, "Doit être > 0");
        return v;
    }
    private double requireRange(Map<String, String> r, String f, double min, double max) {
        try {
            double v = Double.parseDouble(requireText(r, f));
            if (v < min || v > max) throw new RowValidationException(f, "Hors de [" + min + ", " + max + "]");
            return v;
        } catch (NumberFormatException e) {
            throw new RowValidationException(f, "Nombre invalide");
        }
    }
    private <E extends Enum<E>> E requireEnum(Map<String, String> r, String f, Class<E> cls) {
        try { return Enum.valueOf(cls, requireText(r, f).toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new RowValidationException(f, "Valeur d'énumération invalide"); }
    }
    private Instant requireInstant(Map<String, String> r, String f) {
        try { return Instant.parse(requireText(r, f)); }
        catch (Exception e) { throw new RowValidationException(f, "Instant ISO-8601 UTC invalide"); }
    }

    private static class RowValidationException extends RuntimeException {
        final String field;
        RowValidationException(String field, String message) { super(message); this.field = field; }
    }
}
