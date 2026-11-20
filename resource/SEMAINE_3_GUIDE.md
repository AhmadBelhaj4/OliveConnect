# OliveConnect — Semaine 3 : guide pas à pas (tous les membres)

> **Sources** : [OliveConnect_Plan_Projet.md](file:///c:/Users/GMI/Desktop/My_Projects/jeee/OliveConnect_Plan_Projet.md) (§1 exigences, §3.3 Redis, §5.5 Dashboard & Import, §6 endpoints S3, §7 tâches A14–A19 / B11–B15 / C13–C18 / COM5–COM6, §8 planning S3, §10 plan de repli), [SEMAINE_1_GUIDE.md](file:///c:/Users/GMI/Desktop/My_Projects/jeee/SEMAINE_1_GUIDE.md), [SEMAINE_2_GUIDE.md](file:///c:/Users/GMI/Desktop/My_Projects/jeee/SEMAINE_2_GUIDE.md) et le design **« Olive Field »** de [reactsemain1update.md](file:///c:/Users/GMI/Desktop/My_Projects/jeee/reactsemain1update.md) / [prototype react](file:///c:/Users/GMI/Desktop/My_Projects/jeee/prototype%20react).

**Objectif de la Semaine 3** (plan §8) : *Mesurer et livrer : dashboard par agrégation, cache Redis, imports, charge à 100 connexions, déploiement Nginx accessible de l'extérieur, sauvegarde restaurée, rapport et soutenance.*

## Périmètre strict de la Semaine 3

| Membre | Tâches (dans l'ordre du plan) | Preuve à livrer vendredi |
|---|---|---|
| **A** | A14 Image Docker backend · A15 Import CSV/JSON · A16 Import planifié · A17 Administration · A18 Durcissement (429 Redis + CORS) · A19 Rapport (A) | Import idempotent (Scénario E) ; administration ; 429 sur la connexion ; image Docker ; sections de rapport |
| **B** | B11 Tableau de bord (4 agrégations) · B12 Cache Redis réel · B13 Plans de requêtes (`explain`) · B14 Test de charge k6 (100 VU / 500 agr.) · B15 Sauvegarde + rapport | Dashboard = fixture calculée à la main ; cache hit / expiration / invalidation ; `docs/perf-report.md` ; sauvegarde et restauration testées |
| **C** | C13 Déploiement v0 (Nginx + HTTPS + VM) · C14 Tableau de bord (Recharts « Olive Field ») · C15 Administration + Import · C16 Aide en ligne + visite guidée · C17 Finitions mobile 360 px · C18 Rapport (C) | URL externe stable accessible depuis un téléphone ; dashboard, admin, aide ; captures 360 px ; sections de rapport |
| **COMMUN** | COM5 Scénarios A à E sur l'URL déployée · COM6 Rapport PDF & répétition soutenance (2 passages) | `docs/test-plan.md` + `evidence/` ; rapport PDF assemblé ; soutenance répétée 2 fois |

**Démo finale du vendredi** (plan §8) — depuis un **téléphone** sur l'**URL de déploiement** :
1. Les **scénarios A à E** complets ;
2. Le **dashboard** avec les valeurs exactes de la fixture ;
3. Le résultat **k6** (100 connexions simultanées, p95 < 10 s, objectif interne < 2 s) ;
4. Une **restauration de sauvegarde** (`mongodump` / `mongorestore`) réussie ;
5. **Redis arrêté**, l'application répond encore (`200 OK` via MongoDB).

> [!CAUTION]
> **Règle de coupure du jeudi soir (plan §8 & §10)** : Tout ce qui n'est pas terminé le **jeudi soir** est coupé en suivant le plan de repli (§7.2). Le **vendredi** est réservé uniquement aux correctifs mineurs, aux scénarios A–E sur la VM, au rapport PDF et aux 2 répétitions de soutenance.

---

## Sommaire

- [0. Avant de commencer & Planning jour par jour (S3)](#0-avant-de-commencer--planning-jour-par-jour-s3)
- [1. MEMBRE A — A14 → A19](#1-membre-a--a14--a19)
- [2. MEMBRE B — B11 → B15](#2-membre-b--b11--b15)
- [3. MEMBRE C — C13 → C18](#3-membre-c--c13--c18)
- [4. COMMUN — COM5 (Scénarios A–E) & COM6 (Rapport + Soutenance)](#4-commun--com5-scénarios-ae--com6-rapport--soutenance)
- [5. Plan de repli du jeudi soir & Checklist finale de soutenance](#5-plan-de-repli-du-jeudi-soir--checklist-finale-de-soutenance)

---

## 0. Avant de commencer & Planning jour par jour (S3)

### 0.1 Point de départ (lundi matin S3)

```powershell
git switch main
git pull origin main
cd backend; .\mvnw.cmd test        # S1 + S2 (scénarios A–D + SecurityMatrixTest) doivent être VERTS
cd ..\frontend; npm run build      # zéro erreur TypeScript / Vite
```

Vérifier aussi que l'**accès SSH à la VM** (demandé en S1 COM1) fonctionne : `ssh <user>@<VM_IP> "docker compose version"`.

### 0.2 Planning jour par jour (A 31 h · B 31 h · C 29 h, communs inclus)

| Jour | COMMUN | Membre A | Membre B | Membre C |
|---|---|---|---|---|
| **J1** | Point 15 min | **A14 Dockerfile backend (2 h, fusion avant midi)** · A15 Import CSV/JSON (début) | B11 Tableau de bord — 4 agrégations (8 h) | **C13 Déploiement v0 : Nginx + compose prod** (début) |
| **J2** | Point 15 min | A15 Import CSV/JSON (fin, idempotence `externalId`) | B12 Cache Redis réel + résilience panne (7 h) | **C13 URL externe sur VM prête dès ce soir** · C14 Dashboard (début) |
| **J3** | Point 15 min | A16 Import planifié `@Scheduled` (3 h) · A17 Admin comptes (4 h) | B13 Plans `explain()` (2 h) · B14 début k6 | C14 Dashboard Recharts (fin) · C15 Admin + Import (4 h) |
| **J4** | **Gel des fonctionnalités jeudi soir** | A18 Durcissement : 429 Redis + CORS (3 h) · A19 Rapport (A) | B14 Test de charge k6 100 VU / 500 agr. (4 h) · B15 Backup/Restore + Rapport (B) | C16 Aide + visite guidée (2 h) · C17 Finitions 360 px (3 h) · C18 Rapport (C) |
| **J5** | **COM5** Scénarios A à E sur la VM + captures (3 h) · **COM6** Rapport PDF + **2 répétitions** soutenance (3 h) | Correctifs / rapport / soutenance | Correctifs / rapport / soutenance | Redéploiement final / rapport / soutenance |

### 0.3 Critères d'acceptation des tâches de la Semaine 3

| Tâche | Resp. | Critère d'acceptation (vérifiable) |
|---|---|---|
| A14 | A | `docker build ./backend` réussit et `/actuator/health` passe à `UP` dans le compose prod. |
| A15 | A | `POST /admin/imports` rejette les lignes invalides avec numéro de ligne + raison ; 2e import du même fichier = `0` doublon (Scénario E). |
| A16 | A | Un fichier déposé dans `sample-data/inbox/` est importé automatiquement par `@Scheduled` puis déplacé dans `sample-data/processed/`. |
| A17 | A | `GET/POST /admin/users`, `PATCH /role`, `PATCH /active` opérationnels ; non-admin → `403`, audit rempli. |
| A18 | A | 6e échec de connexion en 15 min (même IP + email) → `429 RATE_LIMITED` ; CORS limité à l'origine du front. |
| B11 | B | Test sur fixture de 10 documents : les 4 endpoints `/dashboard/*` renvoient exactement les valeurs calculées à la main. |
| B12 | B | Tests : hit Redis, expiration TTL, invalidation après écriture MongoDB, **Redis arrêté → 200 OK**. |
| B13 | B | Sorties `explain("executionStats")` avant/après (`IXSCAN` au lieu de `COLLSCAN`) consignées dans le rapport. |
| B14 | B | Script k6 (100 VU sur `--seed --farmers=500`) exécuté ; `docs/perf-report.md` contient p95, taux d'erreur et conditions. |
| B15 | B | `mongodump` exécuté, collection supprimée, `mongorestore` restauré avec succès ; `docs/deployment.md` à jour. |
| C13 | C | Application accessible depuis un téléphone via l'URL Nginx de la VM ; MongoDB/Redis non exposés. |
| C14 | C | Dashboard « Olive Field » (KPI + 4 graphiques Recharts) affiche les valeurs exactes de B11. |
| C15 | C | Écrans Admin (comptes/rôles) + Upload import CSV/JSON + tableau des erreurs rejetées branchés. |
| C16 | C | Page d'aide `/help` accessible + visite guidée lancée automatiquement au premier login d'un nouveau compte. |
| C17 | C | Aucun débordement horizontal à `360 px` ; captures mobile dans `evidence/`. |
| COM5–6 | Tous | `docs/test-plan.md` + `evidence/` complets ; rapport PDF relu ; 2 passages de soutenance chronométrés. |

---

## 1. MEMBRE A — A14 → A19

---

### A14 · Image Docker du backend (2 h) — **J1, fusion avant midi (débloque C13)**

**Branche** : `feature/docker-backend-a14` · **Preuve** : `docker build -t oliveconnect-backend ./backend` démarre et répond `UP` sur `/actuator/health`.

#### Étape 1 — `backend/Dockerfile` (multi-stage build léger Java 21)

```dockerfile
# Stage 1 : compilation Maven avec mise en cache des dépendances
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Stage 2 : exécution JRE 21 (utilisateur non-root + healthcheck)
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/inbox /app/processed && chown -R app:app /app
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
```

#### Étape 2 — `backend/.dockerignore`

```text
target/
.git/
*.log
```

#### Étape 3 — Variables d'environnement documentées (noms uniquement, à mettre dans `docs/deployment.md`)

| Variable | Rôle | Obligatoire |
|---|---|---|
| `MONGODB_URI` | Chaîne de connexion MongoDB (réseau interne Docker en prod) | Oui |
| `REDIS_HOST` / `REDIS_PORT` | Hôte et port Redis (`redis`, `6379` en prod) | Oui |
| `JWT_SECRET` | Secret HS256 (≥ 32 octets aléatoires) | Oui |
| `CORS_ALLOWED_ORIGINS` | URL publique exacte du front (ex. `https://oliveconnect.example.tn`) | Oui |
| `OLIVECONNECT_IMPORT_INBOX` | Dossier surveillé par l'import planifié (`/app/inbox` en prod) | Non |

---

### A15 · Import CSV / JSON validé & idempotent (9 h) — J1–J2

**Branche** : `feature/imports-a15` · **Preuve** : Scénario E — importer **2 fois** le même fichier crée **0 doublon** (`updatedCount` remplace `insertedCount`), les lignes invalides sont rejetées avec `{row, field, message}` sans insertion partielle.

Endpoints (§6) : `POST /api/v1/admin/imports` (`ADMIN`, multipart champs `type` et `file`, `201`) · `GET /api/v1/admin/imports` (`ADMIN`, `200`) · `GET /api/v1/admin/imports/{id}` (`ADMIN`, `200 / 404`).

#### Étape 1 — Index uniques sur `externalId` (PR relue par B dans `MongoIndexConfig`)

```java
// Ajouter dans MongoIndexConfig.ensureIndexes() (sparse = autorise les documents créés via l'API sans externalId)
for (String col : List.of("orchards", "workers", "vehicles", "presses", "maturity_readings", "weather_observations")) {
    mongo.indexOps(col).createIndex(
            new Index().on("externalId", Sort.Direction.ASC).unique().sparse()
                    .named("idx_" + col + "_external_id_uq"));
}
```

#### Étape 2 — Limite de taille multipart dans `application.yml`

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB

oliveconnect:
  import:
    inbox-dir: ${OLIVECONNECT_IMPORT_INBOX:../sample-data/inbox}
    processed-dir: ${OLIVECONNECT_IMPORT_PROCESSED:../sample-data/processed}
    max-bytes: 5242880
```

#### Étape 3 — Document, Repository & DTO (`importer/`)

```java
// importer/document/ImportJobDocument.java
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
```

```java
// importer/repository/ImportJobRepository.java
package tn.oliveconnect.importer.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.oliveconnect.importer.document.ImportJobDocument;

public interface ImportJobRepository extends MongoRepository<ImportJobDocument, String> {}
```

```java
// importer/dto/ImportJobResponse.java — 11 clés exactement (§5.5)
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
```

#### Étape 4 — Service d'import (`importer/service/ImportService.java`)

```java
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
        this.jobs = jobs; this.users = users; this.mongo = mongo; this.mapper = mapper;
        this.cache = cache; this.maxBytes = maxBytes;
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
        int inserted = 0, updated = 0, rejected = 0;
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

        // Étape 6 des règles d'import (§5.5) : invalider Redis
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
```

#### Étape 5 — Controller (`importer/controller/ImportController.java`) & Test Scénario E

```java
package tn.oliveconnect.importer.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.oliveconnect.common.enums.ImportType;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.importer.dto.ImportJobResponse;
import tn.oliveconnect.importer.service.ImportService;

@RestController
@RequestMapping("/api/v1/admin/imports")
@PreAuthorize("hasRole('ADMIN')")
public class ImportController {
    private final ImportService service;
    public ImportController(ImportService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportJobResponse upload(@RequestParam ImportType type, @RequestPart("file") MultipartFile file) {
        return service.importMultipart(type, file);
    }

    @GetMapping
    public PageResponse<ImportJobResponse> list(
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public ImportJobResponse get(@PathVariable String id) { return service.get(id); }
}
```

```java
// src/test/java/tn/oliveconnect/importer/ImportIntegrationTest.java — preuve Scénario E
package tn.oliveconnect.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

class ImportIntegrationTest extends AbstractIntegrationTest {

    @Test
    void scenarioE_partialImport_thenReimportProducesZeroDuplicates() throws Exception {
        bearer(Role.FARMER, "farmer@example.test");
        String admin = bearer(Role.ADMIN, "admin@example.test");

        String csv = """
                externalId,farmerEmail,name,areaHectares,oliveVariety,longitude,latitude,publicVisibility
                EXT-O1,farmer@example.test,Verger Import 1,3.5,Chemlali,10.76,34.73,true
                EXT-O2,farmer@example.test,Verger Latitude Ko,2.0,Chemlali,10.76,120.0,true
                """;
        var file = new MockMultipartFile("file", "orchards.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        // 1er import : 1 inséré, 1 rejeté (latitude hors [-90, 90]) -> PARTIAL
        String json1 = mvc.perform(multipart("/api/v1/admin/imports").file(file)
                        .param("type", "ORCHARDS").header("Authorization", admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.insertedCount").value(1))
                .andExpect(jsonPath("$.rejectedCount").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("latitude"))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json1, "id", "type", "sourceFile", "status", "startedAt", "finishedAt",
                "readCount", "insertedCount", "updatedCount", "rejectedCount", "errors");

        // 2e import du même fichier : 0 inséré, 1 mis à jour -> 0 doublon !
        mvc.perform(multipart("/api/v1/admin/imports").file(file)
                        .param("type", "ORCHARDS").header("Authorization", admin))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.insertedCount").value(0))
           .andExpect(jsonPath("$.updatedCount").value(1));

        assertThat(mongo.getCollection("orchards").countDocuments()).isEqualTo(1);
    }
}
```

---

### A16 · Import planifié `@Scheduled` sur `sample-data/inbox` (3 h) — J3

**Branche** : `feature/scheduled-import-a16` · **Preuve** : un fichier `orchards_*.csv` déposé dans `sample-data/inbox/` est importé automatiquement sans action manuelle puis déplacé dans `sample-data/processed/`.

1. Ajouter `@EnableScheduling` sur `OliveConnectApplication.java`.
2. Créer `importer/service/ScheduledInboxImporter.java` :

```java
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
```

3. Placer les fichiers d'exemple dans `sample-data/examples/` (`orchards_sample.csv`, `workers_sample.csv`, `vehicles_sample.csv`, `presses_sample.json`, `maturity_readings_sample.csv`, `weather_observations_sample.json`, `press_capacity_sample.csv`).

---

### A17 · Administration des comptes (4 h) — J3–J4

**Branche** : `feature/admin-users-a17` · **Preuve** : `GET/POST /admin/users`, `PATCH /role`, `PATCH /active` ; non-admin → `403`, audit enregistré.

```java
// user/dto/AdminUserDtos.java
package tn.oliveconnect.user.dto;

import jakarta.validation.constraints.*;
import tn.oliveconnect.common.enums.Role;

public final class AdminUserDtos {
    private AdminUserDtos() {}
    public record AdminCreateUserRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            @Size(max = 30) String phone,
            @NotNull Role role) {}
    public record UpdateRoleRequest(@NotNull Role role) {}
    public record UpdateActiveRequest(@NotNull Boolean active) {}
}
```

```java
// user/controller/AdminUserController.java
package tn.oliveconnect.user.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.audit.service.AuditService;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.security.AuthUser;
import tn.oliveconnect.user.document.UserDocument;
import tn.oliveconnect.user.dto.AdminUserDtos.*;
import tn.oliveconnect.user.dto.UserResponse;
import tn.oliveconnect.user.repository.UserRepository;
import tn.oliveconnect.user.service.AuthService;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AdminUserController(UserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users; this.encoder = encoder; this.audit = audit;
    }

    @GetMapping
    public PageResponse<UserResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(users.findAll(pageable), UserResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@AuthenticationPrincipal AuthUser admin, @Valid @RequestBody AdminCreateUserRequest req) {
        String email = AuthService.normalizeEmail(req.email());
        if (users.existsByEmail(email)) throw ApiException.duplicate("email", "Cet email est déjà utilisé.");
        UserDocument u = new UserDocument();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setPhone(req.phone());
        u.setRole(req.role());
        u.setActive(true);
        u = users.save(u);
        audit.record(admin, "USER_CREATED", "USER", u.getId(), "role=" + u.getRole());
        return UserResponse.from(u);
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                   @Valid @RequestBody UpdateRoleRequest req) {
        UserDocument u = users.findById(id).orElseThrow(() -> ApiException.notFound("Utilisateur introuvable."));
        u.setRole(req.role());
        u = users.save(u);
        audit.record(admin, "ROLE_CHANGED", "USER", u.getId(), "newRole=" + req.role());
        return UserResponse.from(u);
    }

    @PatchMapping("/{id}/active")
    public UserResponse updateActive(@AuthenticationPrincipal AuthUser admin, @PathVariable String id,
                                     @Valid @RequestBody UpdateActiveRequest req) {
        UserDocument u = users.findById(id).orElseThrow(() -> ApiException.notFound("Utilisateur introuvable."));
        u.setActive(req.active());
        u = users.save(u);
        audit.record(admin, "USER_ACTIVE_CHANGED", "USER", u.getId(), "active=" + req.active());
        return UserResponse.from(u);
    }
}
```

---

### A18 · Durcissement : limitation des connexions (429 Redis) & CORS (3 h) — J4

**Branche** : `feature/security-hardening-a18` · **Preuve** : test — **6e échec** de connexion en 15 min pour même IP + email → **`429 RATE_LIMITED`** ; si Redis est arrêté, la connexion reste fonctionnelle.

Clé figée (§3.3) : `oliveconnect:rl:login:<ip+email>`, TTL **15 min**, seuil **5 échecs**.

```java
// security/LoginRateLimiter.java
package tn.oliveconnect.security;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.error.ErrorCode;

@Component
public class LoginRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;
    public LoginRateLimiter(StringRedisTemplate redis) { this.redis = redis; }

    private String key(String ip, String email) {
        return "oliveconnect:rl:login:" + ip + "+" + email;
    }

    public void checkAllowed(String ip, String email) {
        try {
            String val = redis.opsForValue().get(key(ip, email));
            if (val != null && Long.parseLong(val) >= MAX_FAILURES) {
                throw new ApiException(ErrorCode.RATE_LIMITED, ErrorCode.RATE_LIMITED.defaultMessage(), null);
            }
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Redis indisponible pour le rate-limit login — poursuite sans blocage");
        }
    }

    public void recordFailure(String ip, String email) {
        try {
            String k = key(ip, email);
            Long count = redis.opsForValue().increment(k);
            if (count != null && count == 1L) redis.expire(k, WINDOW);
        } catch (RuntimeException e) {
            log.warn("Redis indisponible (recordFailure ignoré)");
        }
    }

    public void reset(String ip, String email) {
        try { redis.delete(key(ip, email)); } catch (RuntimeException ignored) {}
    }
}
```

Brancher dans `AuthController.login` (passer `request.getRemoteAddr()`) et `AuthService.login` : appeler `checkAllowed` avant de vérifier BCrypt, `recordFailure` en cas d'échec, `reset` en cas de succès. Vérifier dans `AuthIntegrationTest` que 5 mauvais mots de passe renvoient `401` et le **6e** renvoie **`429 RATE_LIMITED`**.

### A19 · Rapport (A) (4 h) — J4–J5

Rédiger dans `docs/report-A-backend-security.md` :
1. Architecture en 4 couches + DTO + façades Java (`HarvestRequestFacade`, `NotificationFacade`, `TourFacade`, `CacheInvalidator`).
2. Sécurité à deux niveaux (JWT HS256, matrice 401/403/404 issue de `target/security-matrix.md`, `audit_events`, rate-limit 429).
3. API de persistance Spring Data MongoDB + mécanisme d'import idempotent (`externalId`).

---

## 2. MEMBRE B — B11 → B15

---

### B11 · Tableau de bord — 4 agrégations `MongoTemplate` (8 h) — J1–J2

**Branche** : `feature/dashboard-b11` · **Preuve** : test sur une fixture de 10 documents avec résultats vérifiés à la main + `assertExactKeys`.

Endpoints (§6) : `GET /api/v1/dashboard/{summary | yield-by-orchard | collection-delays | bin-fill-rates}?from=2026-11-01&to=2026-11-30` (`COLLECTION_MANAGER, PRESS_MANAGER, ADMIN`, `200 / 400 / 403`).

#### Étape 1 — DTOs (`dashboard/dto/DashboardDtos.java`) — clés **exactes** de §5.5

```java
package tn.oliveconnect.dashboard.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record DashboardSummaryResponse(
            LocalDate from, LocalDate to, double totalHarvestedKg, double totalReceivedKg,
            long completedTours, long openAlerts, Map<String, Long> requestsByStatus) {}

    public record OrchardYieldItem(String orchardId, String orchardName, double totalHarvestedKg, long collectionCount) {}
    public record YieldByOrchardResponse(LocalDate from, LocalDate to, List<OrchardYieldItem> items) {}

    public record CollectionDelaysResponse(
            LocalDate from, LocalDate to, long sampleSize,
            Double avgSubmittedToScheduledHours, Double avgScheduledToCompletedHours) {}

    public record BinFillRateItem(String binId, String binCode, int capacityKg, long receptionCount, Double avgFillRate) {}
    public record BinFillRatesResponse(LocalDate from, LocalDate to, List<BinFillRateItem> items) {}
}
```

#### Étape 2 — Service d'agrégations (`dashboard/service/DashboardService.java`)

```java
package tn.oliveconnect.dashboard.service;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import tn.oliveconnect.cache.CacheService;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.dashboard.dto.DashboardDtos.*;

@Service
public class DashboardService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(90); // §3.3 : 90 s
    private final MongoTemplate mongo;
    private final CacheService cache;

    public DashboardService(MongoTemplate mongo, CacheService cache) {
        this.mongo = mongo; this.cache = cache;
    }

    public DashboardSummaryResponse summary(LocalDate from, LocalDate to) {
        validateRange(from, to);
        String key = "oliveconnect:dashboard:summary:" + from + "_" + to;
        return cache.getOrLoad(key, CACHE_TTL, DashboardSummaryResponse.class, () -> computeSummary(from, to));
    }

    public YieldByOrchardResponse yieldByOrchard(LocalDate from, LocalDate to) {
        validateRange(from, to);
        String key = "oliveconnect:dashboard:yield:" + from + "_" + to;
        return cache.getOrLoad(key, CACHE_TTL, YieldByOrchardResponse.class, () -> computeYield(from, to));
    }

    public CollectionDelaysResponse collectionDelays(LocalDate from, LocalDate to) {
        validateRange(from, to);
        String key = "oliveconnect:dashboard:delays:" + from + "_" + to;
        return cache.getOrLoad(key, CACHE_TTL, CollectionDelaysResponse.class, () -> computeDelays(from, to));
    }

    public BinFillRatesResponse binFillRates(LocalDate from, LocalDate to) {
        validateRange(from, to);
        String key = "oliveconnect:dashboard:bins:" + from + "_" + to;
        return cache.getOrLoad(key, CACHE_TTL, BinFillRatesResponse.class, () -> computeBinFillRates(from, to));
    }

    // --- 1. SUMMARY ---
    private DashboardSummaryResponse computeSummary(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Total récolté (demandes COMPLETED sur la période)
        Aggregation harvestedAgg = newAggregation(
                match(Criteria.where("status").is("COMPLETED").and("completedAt").gte(start).lt(end)),
                group().sum(ConvertOperators.ToDouble.toDouble("$actualQuantityKg")).as("total"));
        Document hDoc = mongo.aggregate(harvestedAgg, "harvest_requests", Document.class).getUniqueMappedResult();
        double totalHarvested = hDoc != null && hDoc.get("total") instanceof Number n ? round1(n.doubleValue()) : 0.0;

        // Total reçu au pressoir
        Aggregation receivedAgg = newAggregation(
                match(Criteria.where("receivedAt").gte(start).lt(end)),
                group().sum(ConvertOperators.ToDouble.toDouble("$weightKg")).as("total"));
        Document rDoc = mongo.aggregate(receivedAgg, "press_receptions", Document.class).getUniqueMappedResult();
        double totalReceived = rDoc != null && rDoc.get("total") instanceof Number n ? round1(n.doubleValue()) : 0.0;

        long completedTours = mongo.count(Query.query(
                Criteria.where("status").is("COMPLETED").and("completedAt").gte(start).lt(end)), "tours");
        long openAlerts = mongo.count(Query.query(Criteria.where("status").is("OPEN")), "field_alerts");

        // Répartition des demandes par statut : TOUTES les 7 clés présentes, à 0 si besoin (§5.5)
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (HarvestStatus st : HarvestStatus.values()) byStatus.put(st.name(), 0L);
        Aggregation statusAgg = newAggregation(
                match(Criteria.where("requestedAt").gte(start).lt(end)),
                group("status").count().as("count"));
        for (Document d : mongo.aggregate(statusAgg, "harvest_requests", Document.class)) {
            if (d.getString("_id") != null) byStatus.put(d.getString("_id"), ((Number) d.get("count")).longValue());
        }
        return new DashboardSummaryResponse(from, to, totalHarvested, totalReceived, completedTours, openAlerts, byStatus);
    }

    // --- 2. YIELD BY ORCHARD ---
    private YieldByOrchardResponse computeYield(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Aggregation agg = newAggregation(
                match(Criteria.where("status").is("COMPLETED").and("completedAt").gte(start).lt(end)),
                group("orchardId")
                        .first("orchardName").as("orchardName")
                        .sum(ConvertOperators.ToDouble.toDouble("$actualQuantityKg")).as("totalHarvestedKg")
                        .count().as("collectionCount"),
                sort(Sort.Direction.DESC, "totalHarvestedKg"));

        List<OrchardYieldItem> items = mongo.aggregate(agg, "harvest_requests", Document.class)
                .getMappedResults().stream()
                .map(d -> new OrchardYieldItem(
                        String.valueOf(d.get("_id")),
                        d.getString("orchardName"),
                        round1(((Number) d.get("totalHarvestedKg")).doubleValue()),
                        ((Number) d.get("collectionCount")).longValue()))
                .toList();
        return new YieldByOrchardResponse(from, to, items);
    }

    // --- 3. COLLECTION DELAYS ---
    private CollectionDelaysResponse computeDelays(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Aggregation agg = newAggregation(
                match(Criteria.where("status").is("COMPLETED")
                        .and("completedAt").gte(start).lt(end)
                        .and("requestedAt").ne(null).and("scheduledAt").ne(null)),
                project()
                        .and(ArithmeticOperators.Divide.valueOf(
                                ArithmeticOperators.Subtract.valueOf("scheduledAt").subtract("requestedAt"))
                                .divideBy(3_600_000.0)).as("subToSched")
                        .and(ArithmeticOperators.Divide.valueOf(
                                ArithmeticOperators.Subtract.valueOf("completedAt").subtract("scheduledAt"))
                                .divideBy(3_600_000.0)).as("schedToComp"),
                group().count().as("sampleSize")
                        .avg("subToSched").as("avgSubToSched")
                        .avg("schedToComp").as("avgSchedToComp"));

        Document d = mongo.aggregate(agg, "harvest_requests", Document.class).getUniqueMappedResult();
        if (d == null) return new CollectionDelaysResponse(from, to, 0, null, null);
        return new CollectionDelaysResponse(from, to,
                ((Number) d.get("sampleSize")).longValue(),
                round1(((Number) d.get("avgSubToSched")).doubleValue()),
                round1(((Number) d.get("avgSchedToComp")).doubleValue()));
    }

    // --- 4. BIN FILL RATES ---
    private BinFillRatesResponse computeBinFillRates(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Aggregation agg = newAggregation(
                match(Criteria.where("receivedAt").gte(start).lt(end)),
                project("binId").and(ConvertOperators.ToDouble.toDouble("$weightKg")).as("weightKg")
                        .and(ConvertOperators.ToObjectId.toObjectId("$binId")).as("binObjId"),
                lookup("bins", "binObjId", "_id", "bin"),
                unwind("bin"),
                project("binId", "weightKg")
                        .and("bin.code").as("binCode")
                        .and("bin.capacityKg").as("capacityKg")
                        .and(ArithmeticOperators.Divide.valueOf("weightKg").divideBy("bin.capacityKg")).as("fillRate"),
                group("binId")
                        .first("binCode").as("binCode")
                        .first("capacityKg").as("capacityKg")
                        .count().as("receptionCount")
                        .avg("fillRate").as("avgFillRate"),
                sort(Sort.Direction.DESC, "avgFillRate"));

        List<BinFillRateItem> items = mongo.aggregate(agg, "press_receptions", Document.class)
                .getMappedResults().stream()
                .map(d -> new BinFillRateItem(
                        d.getString("_id"),
                        d.getString("binCode"),
                        ((Number) d.get("capacityKg")).intValue(),
                        ((Number) d.get("receptionCount")).longValue(),
                        Math.round(((Number) d.get("avgFillRate")).doubleValue() * 100.0) / 100.0))
                .toList();
        return new BinFillRatesResponse(from, to, items);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw ApiException.validation("from", "from et to obligatoires");
        if (from.isAfter(to)) throw ApiException.validation("from", "from doit être <= to");
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw ApiException.validation("to", "Période maximale autorisée : 366 jours");
        }
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
```

#### Étape 3 — Controller (`dashboard/controller/DashboardController.java`) & Test Fixture

```java
package tn.oliveconnect.dashboard.controller;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.oliveconnect.dashboard.dto.DashboardDtos.*;
import tn.oliveconnect.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAnyRole('COLLECTION_MANAGER','PRESS_MANAGER','ADMIN')")
public class DashboardController {

    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(from, to);
    }

    @GetMapping("/yield-by-orchard")
    public YieldByOrchardResponse yieldByOrchard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.yieldByOrchard(from, to);
    }

    @GetMapping("/collection-delays")
    public CollectionDelaysResponse collectionDelays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.collectionDelays(from, to);
    }

    @GetMapping("/bin-fill-rates")
    public BinFillRatesResponse binFillRates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.binFillRates(from, to);
    }
}
```

> **Preuve B11** : dans `DashboardIntegrationTest.java`, insérer une fixture de **10 documents** (demandes `COMPLETED`/`SUBMITTED`, 2 tournées `COMPLETED`, 1 alerte `OPEN`, 2 bennes + 2 réceptions de `400 kg` et `450 kg` dans une benne de `500 kg` → `avgFillRate = 0.85`) et vérifier que les 4 endpoints retournent les valeurs exactes calculées à la main, ainsi que `400` si `to - from > 366 jours` et `403` pour un `FARMER`.

---

### B12 · Cache Redis réel & résilience panne (7 h) — J2–J3

**Branche** : `feature/redis-cache-b12` · **Preuve** : tests — hit Redis, expiration TTL, invalidation après écriture MongoDB, **Redis arrêté → 200 OK**.

#### Étape 1 — `cache/CacheService.java` (cache-aside résilient + hash SHA-256 stable)

```java
package tn.oliveconnect.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Cache-aside jetable (§3.3) : si Redis tombe, l'application lit MongoDB et renvoie 200 sans jamais planter.
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public CacheService(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis; this.mapper = mapper;
    }

    public <T> T getOrLoad(String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) return mapper.readValue(cached, type);
        } catch (Exception e) {
            log.warn("Lecture cache Redis ignorée pour {} : {}", key, e.getMessage());
        }

        T fresh = loader.get();   // MongoDB = source de vérité

        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(fresh), ttl);
        } catch (Exception e) {
            log.warn("Écriture cache Redis ignorée pour {} : {}", key, e.getMessage());
        }
        return fresh;
    }

    public static String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
```

#### Étape 2 — `cache/RedisCacheInvalidator.java` (remplace `NoOpCacheInvalidator` de S1)

Supprimer `NoOpCacheInvalidator.java` (B4) et le remplacer par :

```java
package tn.oliveconnect.cache;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tn.oliveconnect.common.facade.CacheInvalidator;

/** Clés figées §3.3 : oliveconnect:map:*, oliveconnect:orchard:summary:<id>, oliveconnect:dashboard:* */
@Component
public class RedisCacheInvalidator implements CacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheInvalidator.class);
    private final StringRedisTemplate redis;

    public RedisCacheInvalidator(StringRedisTemplate redis) { this.redis = redis; }

    @Override
    public void orchardChanged(String orchardId) {
        safeDelete("oliveconnect:orchard:summary:" + orchardId);
    }

    @Override
    public void mapChanged() {
        safeDeletePattern("oliveconnect:map:*");
    }

    @Override
    public void dashboardChanged() {
        safeDeletePattern("oliveconnect:dashboard:*");
    }

    private void safeDelete(String key) {
        try { redis.delete(key); } catch (RuntimeException e) { log.warn("Invalidation Redis ignorée ({})", key); }
    }

    private void safeDeletePattern(String pattern) {
        try {
            Set<String> keys = redis.keys(pattern);
            if (keys != null && !keys.isEmpty()) redis.delete(keys);
        } catch (RuntimeException e) {
            log.warn("Invalidation Redis ignorée ({})", pattern);
        }
    }
}
```

#### Étape 3 — Brancher `CacheService` dans `OrchardMapService` (TTL **45 s**, périmètre dans la clé)

```java
// Dans OrchardMapService.findInBbox(...) :
String scope = isStaff ? "STAFF" : (isFarmer ? "FARMER_" + viewer.id() : "PUBLIC");
String filterHash = CacheService.sha256(bboxParam + "|" + maturity + "|" + variety + "|" + harvestStatusFilter);
String cacheKey = "oliveconnect:map:" + scope + ":" + filterHash;
return cache.getOrLoad(cacheKey, Duration.ofSeconds(45), OrchardMapFeatureCollection.class,
        () -> queryMongoMap(viewer, bboxParam, maturity, variety, harvestStatusFilter));
```

> **Sécurité cache (§10 Risques)** : le périmètre (`PUBLIC`, `FARMER_<id>`, `STAFF`) fait **partie de la clé Redis**, donc les données privées d'un agriculteur ne fuitent jamais dans le cache public !

---

### B13 · Plans de requêtes `explain("executionStats")` (2 h) — J3

**Branche** : `feature/explain-b13` · **Preuve** : tableau avant / après dans `docs/perf-report.md` prouvant que MongoDB utilise `IXSCAN` / `GEO_NEAR_2DSPHERE` (et non `COLLSCAN`) sur la base seedée à 500 agriculteurs.

```javascript
// Exécuter dans mongosh après --seed --farmers=500 :
// 1. Carte 2dsphere
db.orchards.find({
  location: { $geoWithin: { $box: [[10.5, 34.5], [11.0, 35.0]] } },
  publicVisibility: true
}).explain("executionStats").executionStats

// 2. File des demandes du responsable
db.harvest_requests.find({ status: "SUBMITTED" })
  .sort({ preferredDate: 1, createdAt: -1 }).limit(20)
  .explain("executionStats").executionStats

// 3. Agrégation dashboard (yield-by-orchard)
db.harvest_requests.explain("executionStats").aggregate([
  { $match: { status: "COMPLETED" } },
  { $group: { _id: "$orchardId", total: { $sum: "$actualQuantityKg" } } }
])
```

Noter dans `docs/perf-report.md` : `winningPlan.stage` (`COLLSCAN` sans index → `IXSCAN` avec index), `totalDocsExamined` et `executionTimeMillis`.

---

### B14 · Test de charge k6 — 100 connexions simultanées sur 500 agriculteurs (4 h) — J4

**Branche** : `feature/k6-loadtest-b14` · **Preuve** : `k6/load-test.js` + rapport réel `docs/perf-report.md`.

1. Seeder la base avec **500 agriculteurs** (~1 000 vergers) :
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--seed --farmers=500"
   ```
2. Créer `k6/load-test.js` :

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '15s', target: 50 },
    { duration: '45s', target: 100 },   // 100 connexions simultanées (exigence sujet)
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],     // < 1 % d'erreur
    http_req_duration: ['p(95)<2000'],  // objectif interne < 2 s (sujet < 10 s)
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export function setup() {
  const res = http.post(`${BASE}/auth/login`, JSON.stringify({
    email: 'collection@example.test',
    password: 'Secret#123',
  }), { headers: { 'Content-Type': 'application/json' } });
  return { token: res.json('accessToken') };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` };

  // 1. Carte publique (bbox Sfax)
  const rMap = http.get(`${BASE}/orchards/map?bbox=10.5,34.5,11.0,35.0`);
  check(rMap, { 'map 200': (r) => r.status === 200 });

  // 2. File des demandes paginée
  const rQueue = http.get(`${BASE}/harvest-requests?status=SUBMITTED&page=0&size=20`, { headers });
  check(rQueue, { 'queue 200': (r) => r.status === 200 });

  // 3. Dashboard summary (caché 90 s dans Redis)
  const rDash = http.get(`${BASE}/dashboard/summary?from=2026-01-01&to=2026-12-31`, { headers });
  check(rDash, { 'dashboard 200': (r) => r.status === 200 });

  sleep(0.5);
}
```

3. Exécuter k6 (sans installation locale grâce à Docker !) :
   ```powershell
   docker run --rm -i -v "${PWD}/k6:/scripts" grafana/k6 run -e BASE_URL=http://host.docker.internal:8080/api/v1 /scripts/load-test.js
   ```
4. Consigner les résultats exacts (sans les embellir) dans `docs/perf-report.md` : machine, CPU/RAM, base (`500` agriculteurs), `p50`, `p95`, `p99`, `http_req_failed`, comparaison **avec Redis vs Redis arrêté**.

---

### B15 · Sauvegardes (`mongodump` / `mongorestore`) + rapport (4 h) — J4–J5

**Branche** : `feature/backups-b15` · **Preuve** : sauvegarde exécutée, suppression contrôlée puis restauration réussie + `docs/deployment.md` (partie données) + `docs/report-B-data-perf.md`.

`infra/scripts/backup-mongo.sh` (planifié par cron sur la VM, ex. tous les jours à 02h00 : `0 2 * * * /opt/oliveconnect/infra/scripts/backup-mongo.sh`) :

```bash
#!/usr/bin/env bash
set -euo pipefail
STAMP=$(date -u +"%Y%m%dT%H%M%SZ")
BACKUP_DIR="/opt/oliveconnect/backups"
mkdir -p "$BACKUP_DIR"

source /opt/oliveconnect/.env
docker exec oliveconnect-mongodb mongodump \
  --uri="mongodb://${MONGO_ROOT_USERNAME}:${MONGO_ROOT_PASSWORD}@localhost:27017/oliveconnect?authSource=admin" \
  --archive --gzip > "${BACKUP_DIR}/oliveconnect-${STAMP}.archive.gz"

# Rétention 7 jours
find "$BACKUP_DIR" -name "oliveconnect-*.archive.gz" -mtime +7 -delete
echo "Sauvegarde OK : ${BACKUP_DIR}/oliveconnect-${STAMP}.archive.gz"
```

**Test de restauration (à capturer pour `evidence/` et la démo du vendredi)** :

```bash
# 1. Noter le nombre de vergers avant
docker exec oliveconnect-mongodb mongosh "mongodb://${MONGO_ROOT_USERNAME}:${MONGO_ROOT_PASSWORD}@localhost:27017/oliveconnect?authSource=admin" --eval "db.orchards.countDocuments()"

# 2. Simuler un incident (vider orchards)
docker exec oliveconnect-mongodb mongosh "mongodb://${MONGO_ROOT_USERNAME}:${MONGO_ROOT_PASSWORD}@localhost:27017/oliveconnect?authSource=admin" --eval "db.orchards.deleteMany({})"

# 3. Restaurer la dernière archive
LATEST=$(ls -t /opt/oliveconnect/backups/oliveconnect-*.archive.gz | head -n1)
cat "$LATEST" | docker exec -i oliveconnect-mongodb mongorestore \
  --uri="mongodb://${MONGO_ROOT_USERNAME}:${MONGO_ROOT_PASSWORD}@localhost:27017/oliveconnect?authSource=admin" \
  --archive --gzip --drop

# 4. Vérifier que le nombre de vergers est revenu à l'identique !
docker exec oliveconnect-mongodb mongosh "mongodb://${MONGO_ROOT_USERNAME}:${MONGO_ROOT_PASSWORD}@localhost:27017/oliveconnect?authSource=admin" --eval "db.orchards.countDocuments()"
```

---

## 3. MEMBRE C — C13 → C18 (Design « Olive Field » & Déploiement VM)

---

### C13 · Déploiement v0 : Nginx, HTTPS & Docker Compose sur la VM (5 h) — **J1–J2**

**Branche** : `feature/deploy-nginx-c13` · **Preuve** : URL externe accessible **depuis un téléphone dès le jour 2** ; MongoDB et Redis non exposés à l'extérieur.

#### Étape 1 — Configuration Nginx (`infra/nginx/default.conf`)

```nginx
server {
    listen 80;
    server_name _;

    # Taille max alignée avec Spring multipart (5 Mo, A15)
    client_max_body_size 6M;

    # En-têtes de sécurité de base
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Build statique React (SPA : try_files renvoie index.html pour React Router)
    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /actuator/health {
        proxy_pass http://backend:8080/actuator/health;
    }

    location /swagger-ui/ {
        proxy_pass http://backend:8080/swagger-ui/;
    }

    location /v3/api-docs {
        proxy_pass http://backend:8080/v3/api-docs;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

#### Étape 2 — `frontend/Dockerfile` (build Vite + Nginx)

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ENV VITE_USE_MOCKS=false
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY ../infra/nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

> Si vous construisez depuis le dossier `frontend/`, placez le contexte Docker à la racine du dépôt (comme dans `compose.prod.yaml` ci-dessous) afin que `COPY infra/nginx/default.conf ...` fonctionne proprement.

#### Étape 3 — `infra/compose.prod.yaml` (MongoDB et Redis **sans aucun port exposé** !)

```yaml
name: oliveconnect-prod

services:
  mongodb:
    image: mongo:8.0
    container_name: oliveconnect-mongodb
    restart: unless-stopped
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_ROOT_USERNAME}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_ROOT_PASSWORD}
      MONGO_INITDB_DATABASE: oliveconnect
    # AUCUNE section ports : MongoDB n'est accessible QUE sur le réseau interne Docker !
    volumes:
      - mongo_data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--quiet", "mongodb://localhost:27017/admin", "--eval", "db.runCommand({ ping: 1 }).ok"]
      interval: 10s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7.4-alpine
    container_name: oliveconnect-redis
    restart: unless-stopped
    command: ["redis-server", "--save", "", "--appendonly", "no"]
    # AUCUNE section ports : Redis reste strictement interne !
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 10

  backend:
    build:
      context: ../backend
      dockerfile: Dockerfile
    container_name: oliveconnect-backend
    restart: unless-stopped
    depends_on:
      mongodb: { condition: service_healthy }
      redis: { condition: service_healthy }
    environment:
      MONGODB_URI: mongodb://${MONGO_ROOT_USERNAME}:${MONGO_ROOT_PASSWORD}@mongodb:27017/oliveconnect?authSource=admin
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: ${JWT_SECRET}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}
      OLIVECONNECT_IMPORT_INBOX: /app/inbox
      OLIVECONNECT_IMPORT_PROCESSED: /app/processed
    volumes:
      - ../sample-data/inbox:/app/inbox
      - ../sample-data/processed:/app/processed

  web:
    build:
      context: ..
      dockerfile: frontend/Dockerfile
    container_name: oliveconnect-web
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "80:80"
      # Pour HTTPS : ajouter "443:443" + montage certificats Let's Encrypt (/etc/letsencrypt) ou reverse-proxy Caddy

volumes:
  mongo_data:
```

> Dans `frontend/Dockerfile` utilisé avec `context: ..`, remplacer `COPY package*.json ./` par `COPY frontend/package*.json ./`, `COPY . .` par `COPY frontend/ .`, et `COPY infra/nginx/default.conf /etc/nginx/conf.d/default.conf`.

#### Étape 4 — Déployer et redéployer sur la VM à chaque fusion sur `main`

```bash
ssh <user>@<VM_IP>
cd /opt/oliveconnect
git pull origin main
docker compose -f infra/compose.prod.yaml --env-file .env up -d --build
docker compose -f infra/compose.prod.yaml ps
curl -s http://localhost/actuator/health    # {"status":"UP"}
```

> **HTTPS** : si la VM possède un nom de domaine, installez Certbot (`certbot --nginx -d <domaine>`) ou placez Caddy devant le port 80/443 ; sinon, générez un certificat TLS auto-signé documenté dans `docs/deployment.md`.

---

### C14 · Tableau de bord Recharts (« Olive Field » branché sur B11) (6 h) — J2–J3

**Branche** : `feature/front-dashboard-c14` · **Preuve** : cartes KPI + filtre de dates (`from`/`to`) + 4 graphiques Recharts reproduisant le design de [prototype react](file:///c:/Users/GMI/Desktop/My_Projects/jeee/prototype%20react/src/components/dashboard) et affichant exactement les valeurs de la fixture de B11.

`frontend/src/features/dashboard/DashboardPage.tsx` :

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Bar, BarChart, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { Factory, Route, TriangleAlert, Wheat } from 'lucide-react';
import { apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import { harvestStatusStyles } from '../_shared/statusStyles';
import type {
  DashboardBinFillRatesResponse,
  DashboardCollectionDelaysResponse,
  DashboardSummaryResponse,
  DashboardYieldByOrchardResponse,
  HarvestStatus,
} from '../../types/contract';

export function DashboardPage() {
  const [from, setFrom] = useState('2026-11-01');
  const [to, setTo] = useState('2026-11-30');
  const qs = `?from=${from}&to=${to}`;

  const summaryQ = useQuery({
    queryKey: ['dashboard', 'summary', from, to],
    queryFn: () => apiFetch<DashboardSummaryResponse>(`/dashboard/summary${qs}`),
  });
  const yieldQ = useQuery({
    queryKey: ['dashboard', 'yield', from, to],
    queryFn: () => apiFetch<DashboardYieldByOrchardResponse>(`/dashboard/yield-by-orchard${qs}`),
  });
  const delaysQ = useQuery({
    queryKey: ['dashboard', 'delays', from, to],
    queryFn: () => apiFetch<DashboardCollectionDelaysResponse>(`/dashboard/collection-delays${qs}`),
  });
  const binsQ = useQuery({
    queryKey: ['dashboard', 'bins', from, to],
    queryFn: () => apiFetch<DashboardBinFillRatesResponse>(`/dashboard/bin-fill-rates${qs}`),
  });

  const s = summaryQ.data;
  const statusPie = s
    ? (Object.entries(s.requestsByStatus) as [HarvestStatus, number][])
        .filter(([, count]) => count > 0)
        .map(([status, value]) => ({
          name: harvestStatusStyles[status].label,
          value,
          color: harvestStatusStyles[status].fg,
        }))
    : [];

  const yieldRows = [...(yieldQ.data?.items ?? [])].slice(0, 7).reverse();
  const maxYield = Math.max(1, ...yieldRows.map((r) => r.totalHarvestedKg));

  return (
    <div className="space-y-5">
      {/* Filtre de dates style TopBar */}
      <GlassCard variant="subtle" className="p-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display font-bold text-xl sm:text-2xl text-olive-900">Tableau de bord des performances</h1>
          <p className="text-xs text-olive-700/75">Agrégations MongoDB + cache Redis (TTL 90 s)</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="h-9 px-3 rounded-lg glass-strong text-xs text-olive-900" />
          <span className="text-xs text-olive-700">→</span>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="h-9 px-3 rounded-lg glass-strong text-xs text-olive-900" />
        </div>
      </GlassCard>

      {/* 4 cartes KPI (style KpiGrid.tsx de prototype react) */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Total récolté', value: s ? `${(s.totalHarvestedKg / 1000).toFixed(1)} t` : '—', icon: Wheat, bg: 'rgba(74,107,42,0.15)', color: '#4a6b2a' },
          { label: 'Total reçu pressoir', value: s ? `${(s.totalReceivedKg / 1000).toFixed(1)} t` : '—', icon: Factory, bg: 'rgba(74,107,42,0.15)', color: '#4a6b2a' },
          { label: 'Tournées terminées', value: s ? String(s.completedTours) : '—', icon: Route, bg: 'rgba(201,150,47,0.15)', color: '#b07f1f' },
          { label: 'Alertes ouvertes', value: s ? String(s.openAlerts) : '—', icon: TriangleAlert, bg: 'rgba(179,58,58,0.15)', color: '#b33a3a' },
        ].map((k) => {
          const Icon = k.icon;
          return (
            <GlassCard key={k.label} hover className="p-5 flex items-start justify-between">
              <div>
                <p className="text-[11px] font-medium uppercase tracking-wide text-olive-700/70">{k.label}</p>
                <p className="mt-2 font-display text-2xl sm:text-3xl font-bold tabnum text-olive-900">{k.value}</p>
              </div>
              <div className="h-9 w-9 rounded-md grid place-items-center shrink-0" style={{ backgroundColor: k.bg, color: k.color }}>
                <Icon className="h-4 w-4" />
              </div>
            </GlassCard>
          );
        })}
      </div>

      {/* Grille 2×2 des 4 graphiques Recharts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* 1. Rendement par verger (style YieldByOrchardChart.tsx) */}
        <GlassCard hover className="p-5">
          <h2 className="font-display font-semibold text-base text-olive-900 mb-3">Rendement par verger (kg)</h2>
          {yieldRows.length === 0 ? (
            <p className="text-sm text-olive-700/70">Aucune donnée sur cette période.</p>
          ) : (
            <div style={{ height: Math.max(220, yieldRows.length * 36) }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={yieldRows} layout="vertical" margin={{ left: 4, right: 16 }}>
                  <XAxis type="number" tick={{ fontSize: 10, fill: '#8b917c' }} axisLine={false} tickLine={false} />
                  <YAxis type="category" dataKey="orchardName" width={115} tick={{ fontSize: 11, fill: '#5e6650' }} axisLine={false} tickLine={false} />
                  <Tooltip />
                  <Bar dataKey="totalHarvestedKg" radius={[0, 4, 4, 0]}>
                    {yieldRows.map((r) => {
                      const ratio = r.totalHarvestedKg / maxYield;
                      const fill = ratio > 0.85 ? '#4a6b2a' : ratio > 0.6 ? '#6b8e3f' : '#a8be72';
                      return <Cell key={r.orchardId} fill={fill} />;
                    })}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </GlassCard>

        {/* 2. Répartition des demandes par statut */}
        <GlassCard hover className="p-5">
          <h2 className="font-display font-semibold text-base text-olive-900 mb-3">Demandes par statut</h2>
          {statusPie.length === 0 ? (
            <p className="text-sm text-olive-700/70">Aucune demande sur cette période.</p>
          ) : (
            <div style={{ height: 240 }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={statusPie} dataKey="value" nameKey="name" innerRadius={55} outerRadius={85} paddingAngle={3}>
                    {statusPie.map((entry) => <Cell key={entry.name} fill={entry.color} />)}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </GlassCard>

        {/* 3. Délais moyens de collecte */}
        <GlassCard hover className="p-5">
          <h2 className="font-display font-semibold text-base text-olive-900 mb-3">
            Délais moyens de collecte ({delaysQ.data?.sampleSize ?? 0} collectes)
          </h2>
          <div className="grid grid-cols-2 gap-3 mt-4">
            <GlassCard variant="subtle" className="p-4">
              <div className="text-xs text-olive-700/75">Soumise → Planifiée</div>
              <div className="font-display font-bold text-2xl text-olive-900 tabnum mt-1">
                {delaysQ.data?.avgSubmittedToScheduledHours != null ? `${delaysQ.data.avgSubmittedToScheduledHours} h` : '—'}
              </div>
            </GlassCard>
            <GlassCard variant="subtle" className="p-4">
              <div className="text-xs text-olive-700/75">Planifiée → Terminée</div>
              <div className="font-display font-bold text-2xl text-olive-900 tabnum mt-1">
                {delaysQ.data?.avgScheduledToCompletedHours != null ? `${delaysQ.data.avgScheduledToCompletedHours} h` : '—'}
              </div>
            </GlassCard>
          </div>
        </GlassCard>

        {/* 4. Taux de remplissage des bennes */}
        <GlassCard hover className="p-5">
          <h2 className="font-display font-semibold text-base text-olive-900 mb-3">Taux de remplissage des bennes</h2>
          {(binsQ.data?.items ?? []).length === 0 ? (
            <p className="text-sm text-olive-700/70">Aucune réception sur cette période.</p>
          ) : (
            <div className="space-y-3">
              {binsQ.data!.items.slice(0, 6).map((b) => {
                const pct = b.avgFillRate != null ? Math.round(b.avgFillRate * 100) : 0;
                return (
                  <div key={b.binId}>
                    <div className="flex justify-between text-xs mb-1 tabnum">
                      <span className="font-semibold text-olive-900">{b.binCode} ({b.capacityKg} kg)</span>
                      <span className="text-olive-700">{pct} % ({b.receptionCount} pesées)</span>
                    </div>
                    <div className="h-2 rounded-full bg-olive-900/10 overflow-hidden">
                      <div className="h-full rounded-full bg-ochre-500" style={{ width: `${Math.min(100, pct)}%` }} />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </GlassCard>
      </div>
    </div>
  );
}
```

---

### C15 · Administration des comptes & Import CSV/JSON (4 h) — J3–J4

**Branche** : `feature/front-admin-c15` · **Preuve** : Scénario E à l'écran (upload d'un CSV, affichage des compteurs `insertedCount / updatedCount / rejectedCount` et du tableau des erreurs par ligne) + gestion des comptes/rôles.

`frontend/src/features/admin/AdminPage.tsx` :

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileUp, Users } from 'lucide-react';
import { ApiClientError, apiFetch } from '../../api/http';
import { GlassCard } from '../../components/GlassCard';
import type { ImportJobResponse, ImportType, PageResponse, Role, UserResponse } from '../../types/contract';

export function AdminPage() {
  const qc = useQueryClient();
  const [importType, setImportType] = useState<ImportType>('ORCHARDS');
  const [file, setFile] = useState<File | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const usersQ = useQuery({
    queryKey: ['admin-users'],
    queryFn: () => apiFetch<PageResponse<UserResponse>>('/admin/users'),
  });
  const importsQ = useQuery({
    queryKey: ['admin-imports'],
    queryFn: () => apiFetch<PageResponse<ImportJobResponse>>('/admin/imports'),
  });

  const uploadMutation = useMutation({
    mutationFn: () => {
      if (!file) throw new Error('Choisissez un fichier');
      const fd = new FormData();
      fd.append('type', importType);
      fd.append('file', file);
      return apiFetch<ImportJobResponse>('/admin/imports', { method: 'POST', body: fd });
    },
    onSuccess: () => {
      setErrorMsg(null);
      setFile(null);
      qc.invalidateQueries({ queryKey: ['admin-imports'] });
    },
    onError: (err) => setErrorMsg(err instanceof ApiClientError ? err.apiError.message : (err as Error).message),
  });

  const roleMutation = useMutation({
    mutationFn: ({ id, role }: { id: string; role: Role }) =>
      apiFetch<UserResponse>(`/admin/users/${id}/role`, { method: 'PATCH', body: JSON.stringify({ role }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-users'] }),
  });

  const activeMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      apiFetch<UserResponse>(`/admin/users/${id}/active`, { method: 'PATCH', body: JSON.stringify({ active }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-users'] }),
  });

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
      {/* Bloc Import CSV / JSON (Scénario E) */}
      <GlassCard className="p-5 lg:col-span-6 space-y-4">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <FileUp className="h-4 w-4" />
          </div>
          <div>
            <h1 className="font-display font-bold text-lg text-olive-900">Import CSV / JSON idempotent</h1>
            <p className="text-xs text-olive-700/75">Upsert sur externalId · rapport des lignes rejetées</p>
          </div>
        </div>

        {errorMsg && <div className="px-3 py-2 rounded-lg bg-[#fee2e2] text-[#b33a3a] text-xs font-medium">{errorMsg}</div>}

        <div className="flex flex-wrap gap-2">
          <select value={importType} onChange={(e) => setImportType(e.target.value as ImportType)} className="h-9 px-3 rounded-lg glass-strong text-xs text-olive-900">
            <option value="ORCHARDS">ORCHARDS</option>
            <option value="WORKERS">WORKERS</option>
            <option value="VEHICLES">VEHICLES</option>
            <option value="PRESSES">PRESSES</option>
            <option value="MATURITY_READINGS">MATURITY_READINGS</option>
            <option value="WEATHER_OBSERVATIONS">WEATHER_OBSERVATIONS</option>
            <option value="PRESS_CAPACITY">PRESS_CAPACITY</option>
          </select>
          <input type="file" accept=".csv,.json" onChange={(e) => setFile(e.target.files?.[0] ?? null)} className="text-xs text-olive-800" />
          <button
            onClick={() => uploadMutation.mutate()}
            disabled={!file || uploadMutation.isPending}
            className="px-4 h-9 rounded-lg bg-olive-600 hover:bg-olive-700 text-cream-50 text-xs font-semibold disabled:opacity-50"
          >
            Importer
          </button>
        </div>

        <div className="space-y-3">
          {importsQ.data?.content.map((j) => (
            <GlassCard key={j.id} variant="subtle" className="p-3.5 space-y-1.5">
              <div className="flex justify-between text-xs font-semibold">
                <span>{j.type} — {j.sourceFile}</span>
                <span className="text-olive-800">{j.status}</span>
              </div>
              <p className="text-xs text-olive-700 tabnum">
                Lues : {j.readCount} · Insérées : <strong>{j.insertedCount}</strong> · Mises à jour : <strong>{j.updatedCount}</strong> · Rejetées : <strong className="text-[#b33a3a]">{j.rejectedCount}</strong>
              </p>
              {j.errors.length > 0 && (
                <ul className="text-[11px] text-[#b33a3a] list-disc ml-4">
                  {j.errors.slice(0, 5).map((e, idx) => (
                    <li key={idx}>Ligne {e.row} ({e.field}) : {e.message}</li>
                  ))}
                </ul>
              )}
            </GlassCard>
          ))}
        </div>
      </GlassCard>

      {/* Bloc Comptes et Rôles */}
      <GlassCard className="p-5 lg:col-span-6 space-y-3">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-lg bg-olive-600/15 text-olive-700 grid place-items-center">
            <Users className="h-4 w-4" />
          </div>
          <h2 className="font-display font-bold text-lg text-olive-900">
            Comptes utilisateurs ({usersQ.data?.totalElements ?? 0})
          </h2>
        </div>

        <div className="space-y-2">
          {usersQ.data?.content.map((u) => (
            <div key={u.id} className="p-3 rounded-xl glass-subtle flex flex-wrap items-center justify-between gap-2">
              <div className="min-w-0">
                <div className="font-semibold text-sm text-olive-900 truncate">{u.fullName}</div>
                <div className="text-xs text-olive-700/75 truncate">{u.email}</div>
              </div>
              <div className="flex items-center gap-2">
                <select
                  value={u.role}
                  onChange={(e) => roleMutation.mutate({ id: u.id, role: e.target.value as Role })}
                  className="h-8 px-2 rounded-lg glass-strong text-xs text-olive-900"
                >
                  {(['FARMER', 'COLLECTION_MANAGER', 'TEAM_LEADER', 'LOGISTICS_MANAGER', 'PRESS_MANAGER', 'ADMIN'] as Role[]).map((r) => (
                    <option key={r} value={r}>{r}</option>
                  ))}
                </select>
                <button
                  onClick={() => activeMutation.mutate({ id: u.id, active: !u.active })}
                  className={`px-2.5 h-8 rounded-lg text-xs font-medium ${u.active ? 'bg-[#d1fae5] text-[#1b5e20]' : 'bg-[#fee2e2] text-[#b33a3a]'}`}
                >
                  {u.active ? 'Actif' : 'Désactivé'}
                </button>
              </div>
            </div>
          ))}
        </div>
      </GlassCard>
    </div>
  );
}
```

---

### C16 · Aide en ligne & visite guidée au premier login (2 h) — J4

**Branche** : `feature/front-help-tour-c16` · **Preuve** : page `/help` accessible à tous + modale de visite guidée lancée automatiquement pour tout nouveau compte (`localStorage` par `user.id`).

```tsx
// frontend/src/components/GuidedTourModal.tsx — exigence du sujet (§1)
import { useEffect, useState } from 'react';
import { HelpCircle, X } from 'lucide-react';
import { useAuth } from '../features/auth/AuthContext';
import { GlassCard } from './GlassCard';

const STEPS = [
  { title: '1. Carte des vergers', text: 'Consultez la maturité des vergers de la région de Sfax en temps réel.' },
  { title: '2. Déclarer un verger & une demande', text: 'Utilisez « Ma position » pour localiser votre verger puis déposez une demande planifiée ou urgente.' },
  { title: '3. Suivi & notifications', text: 'Suivez chaque étape (APPROVED → SCHEDULED → IN_PROGRESS → COMPLETED) et signalez toute alerte terrain.' },
];

export function GuidedTourModal() {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const [step, setStep] = useState(0);

  useEffect(() => {
    if (!user) return;
    const key = `oliveconnect_tour_done_${user.id}`;
    if (!localStorage.getItem(key)) setOpen(true);
  }, [user]);

  if (!open || !user) return null;
  const finish = () => {
    localStorage.setItem(`oliveconnect_tour_done_${user.id}`, '1');
    setOpen(false);
  };

  return (
    <div className="fixed inset-0 z-50 bg-olive-900/40 backdrop-blur-sm grid place-items-center p-4">
      <GlassCard variant="strong" className="max-w-md w-full p-6 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 font-display font-bold text-olive-900">
            <HelpCircle className="h-5 w-5 text-olive-600" /> Visite guidée OliveConnect
          </div>
          <button onClick={finish} aria-label="Fermer"><X className="h-4 w-4 text-olive-700" /></button>
        </div>
        <div>
          <h3 className="font-semibold text-base text-olive-900">{STEPS[step].title}</h3>
          <p className="text-sm text-olive-800 mt-1">{STEPS[step].text}</p>
        </div>
        <div className="flex justify-between items-center pt-2">
          <span className="text-xs text-olive-700 tabnum">Étape {step + 1} / {STEPS.length}</span>
          <div className="flex gap-2">
            {step < STEPS.length - 1 ? (
              <button onClick={() => setStep(step + 1)} className="px-4 h-9 rounded-lg bg-olive-600 text-cream-50 text-xs font-semibold">
                Suivant
              </button>
            ) : (
              <button onClick={finish} className="px-4 h-9 rounded-lg bg-olive-600 text-cream-50 text-xs font-semibold">
                Commencer
              </button>
            )}
          </div>
        </div>
      </GlassCard>
    </div>
  );
}
```

Ajouter `<GuidedTourModal />` dans `Layout.tsx`, créer `HelpPage.tsx` sur `/help` (accessible depuis le menu public) avec le guide des 7 rôles, et brancher `/dashboard` → `<DashboardPage />` et `/admin/users` → `<AdminPage />` dans `App.tsx`.

### C17 & C18 · Finitions mobile `360 px` (3 h) + Rapport C (3 h) — J4–J5

- Ouvrir DevTools Chrome → mode responsive **360 × 800 px** : vérifier l'absence de scroll horizontal sur Login, Carte, Mes vergers, Mes demandes, Dashboard et Admin ; enregistrer les captures dans `evidence/mobile-360px-*.png`.
- Rédiger `docs/report-C-ux-manual.md` : étude préalable, besoins, cas d'utilisation et manuel utilisateur illustré.

---

## 4. COMMUN — COM5 & COM6 (Vendredi S3)

### COM5 · Jouer les 5 scénarios A à E sur l'URL déployée (3 h chacun)

Exécuter ensemble sur l'URL de la VM et sauvegarder chaque capture dans `evidence/` + la grille dans `docs/test-plan.md` :

| Scénario | Vérification sur l'URL déployée (VM) | Preuve à capturer dans `evidence/` |
|---|---|---|
| **A (Flux complet F1+F3)** | Agriculteur → Verger → Demande → Resp. collecte approuve & planifie → Chef démarre & saisit `2380 kg` → Pressoir reçoit → Agriculteur voit `COMPLETED` + notification | `scenario-A-completed.png` + `scenario-A-notif.png` |
| **B (Alertes F2)** | Agriculteur signale alerte `HIGH` → Resp. collecte `acknowledge` puis `resolve` avec note → Agriculteur voit `RESOLVED` | `scenario-B-alert-resolved.png` |
| **C (Sécurité)** | Sans token (`401`), mauvais rôle (`403`), verger d'un autre agriculteur (`404`), 6e échec login (`429`) | `scenario-C-security.png` + `target/security-matrix.md` |
| **D (Conflit 409)** | Même tracteur planifié 2 fois le même jour → `409 RESOURCE_CONFLICT` affiché dans le bandeau rouge | `scenario-D-conflict-409.png` |
| **E (Import idempotent)** | Upload `orchards_sample.csv` (1 ligne valide + 1 invalide → `PARTIAL`) puis ré-upload du même fichier → `0` inséré, `1` mis à jour (`0` doublon) | `scenario-E-import-idempotent.png` |
| **Résilience Redis** | `docker compose -f infra/compose.prod.yaml stop redis` → rafraîchir carte & dashboard → **`200 OK`** | `resilience-redis-down-200.png` |
| **Backup & Restore** | `backup-mongo.sh` → drop `orchards` → `mongorestore` → nombre de vergers restauré | `backup-restore-proof.txt` |

### COM6 · Assemblage du rapport PDF & 2 répétitions de soutenance (3 h chacun)

Plan imposé du rapport (plan §1) — fusionner les sections rédigées au fil des 3 semaines :
1. **Étude préalable & besoins** (`docs/requirements.md`, `docs/use-cases.md` — C).
2. **Conception en couches & contrat API v1** (`docs/class-diagram.md`, `docs/api-contract.md`, `docs/report-A-backend-security.md` — A).
3. **Schéma NoSQL, règles UML→NoSQL, index & cache Redis** (`docs/nosql-model.md`, `docs/report-B-data-perf.md` — B).
4. **Tests, sécurité & performances k6** (`docs/test-plan.md`, `target/security-matrix.md`, `docs/perf-report.md` — A & B).
5. **Déploiement, sauvegardes & manuel utilisateur** (`docs/deployment.md`, `docs/report-C-ux-manual.md`, captures `evidence/` — B & C).

**Soutenance (2 répétitions chronométrées)** : chaque membre doit être capable de répondre à une question sur **le module d'un autre** (d'où les revues croisées et présentations de 5 min des vendredis S1 et S2).

---

## 5. Plan de repli du jeudi soir & Checklist finale (plan §10)

### 5.1 Si le temps manque jeudi soir, couper dans cet ordre strict

1. **F5 Dashboard** : garder uniquement `summary` et `yield-by-orchard` (couper `collection-delays` et `bin-fill-rates`).
2. **Import planifié `@Scheduled` (A16)** : garder l'import manuel via `POST /admin/imports` (A15).
3. **`PATCH /harvest-requests/{id}` et `POST /alerts/{id}/dismiss`**.
4. **Visite guidée (modale)** : garder la page `/help` (exigée par le sujet).
5. **Rate-limit 429 par Redis (A18)**.

> **On ne coupe JAMAIS** : le flux de bout en bout (Scénario A), les tests `401 / 403 / 404 / 409`, le déploiement externe sur la VM, les fonctionnalités **F1 à F4** et le **rapport PDF**.

### 5.2 Checklist finale du vendredi (avant la soutenance)

- [ ] URL de la VM accessible depuis un téléphone en 4G/Wi-Fi.
- [ ] MongoDB et Redis **non exposés** depuis l'extérieur (`nmap` ou `ss -tlnp` sur la VM : seuls 22, 80/443 ouverts).
- [ ] Scénarios **A, B, C, D, E** joués avec succès + captures dans `evidence/`.
- [ ] Test **Redis arrêté → 200 OK** validé puis Redis redémarré.
- [ ] Test **sauvegarde `mongodump` + restauration `mongorestore`** prouvé.
- [ ] Rapport k6 (100 VU / 500 agriculteurs, p95 mesuré) dans `docs/perf-report.md`.
- [ ] Aucun `.env` ni secret dans l'historique Git (`git ls-files | Select-String "\.env$"` vide).
- [ ] Rapport PDF exporté, relu par les 3 membres, et soutenance répétée 2 fois.
