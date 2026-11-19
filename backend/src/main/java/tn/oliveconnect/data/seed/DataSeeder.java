package tn.oliveconnect.data.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.oliveconnect.common.enums.*;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;
import tn.oliveconnect.harvest.repository.HarvestRequestRepository;
import tn.oliveconnect.orchard.document.OrchardDocument;
import tn.oliveconnect.orchard.repository.OrchardRepository;
import tn.oliveconnect.resource.document.*;
import tn.oliveconnect.resource.repository.*;
import tn.oliveconnect.user.document.UserDocument;
import tn.oliveconnect.user.repository.UserRepository;

/**
 * S'exécute uniquement si l'argument "--seed" est passé (ex. --seed --farmers=20 ou --seed --farmers=500).
 * Idempotent : vide et recrée les données de démo.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String[] VARIETIES = {"Chemlali", "Chetoui", "Oueslati", "Zalmati"};
    private static final MaturityStatus[] MATURITIES = MaturityStatus.values();

    private final MongoTemplate mongo;
    private final UserRepository users;
    private final OrchardRepository orchards;
    private final HarvestRequestRepository requests;
    private final WorkerRepository workers;
    private final VehicleRepository vehicles;
    private final BinRepository bins;
    private final EquipmentRepository equipment;
    private final PasswordEncoder encoder;

    public DataSeeder(MongoTemplate mongo, UserRepository users, OrchardRepository orchards,
                      HarvestRequestRepository requests, WorkerRepository workers,
                      VehicleRepository vehicles, BinRepository bins, EquipmentRepository equipment,
                      PasswordEncoder encoder) {
        this.mongo = mongo; this.users = users; this.orchards = orchards; this.requests = requests;
        this.workers = workers; this.vehicles = vehicles; this.bins = bins; this.equipment = equipment;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> raw = Arrays.asList(args.getSourceArgs());
        boolean requested = args.containsOption("seed") || raw.contains("--seed") || raw.contains("seed");
        if (!requested) return;

        int farmersCount = parseFarmersCount(args, raw);
        log.info("Seed OliveConnect démarré (--farmers={})...", farmersCount);

        for (String col : List.of("users", "orchards", "harvest_requests", "workers", "vehicles", "bins", "equipment", "presses")) {
            mongo.getCollection(col).deleteMany(new Document());
        }

        String hash = encoder.encode("Secret#123");

        // 1. Un compte de référence par rôle (menus de C3)
        UserDocument mainFarmer = saveUser("Example Farmer", "farmer@example.test", Role.FARMER, hash);
        saveUser("Resp. Collecte", "collection@example.test", Role.COLLECTION_MANAGER, hash);
        saveUser("Chef Équipe 01", "leader@example.test", Role.TEAM_LEADER, hash);
        saveUser("Resp. Logistique", "logistics@example.test", Role.LOGISTICS_MANAGER, hash);
        saveUser("Resp. Pressoir", "press@example.test", Role.PRESS_MANAGER, hash);
        saveUser("Administrateur", "admin@example.test", Role.ADMIN, hash);

        // 2. Agriculteurs + vergers autour de Sfax [10.76, 34.73] + demandes
        Random rnd = new Random(2026L);   // graine fixe -> données reproductibles
        createOrchardsForFarmer(mainFarmer.getId(), 0, 2, rnd);
        for (int i = 2; i <= farmersCount; i++) {
            UserDocument f = saveUser("Agriculteur %03d".formatted(i),
                    "farmer%03d@example.test".formatted(i), Role.FARMER, hash);
            createOrchardsForFarmer(f.getId(), i, 2, rnd);
        }

        // 3. Ressources : 15 ouvriers, 6 véhicules, 20 bennes, 3 matériels
        for (int i = 1; i <= 15; i++) {
            WorkerDocument w = new WorkerDocument();
            w.setFullName("Ouvrier %02d".formatted(i));
            w.setPhone("+216 74 %03d %03d".formatted(100 + i, 200 + i));
            w.setSkills(i % 3 == 0 ? List.of("PICKING", "TRACTOR") : List.of("PICKING"));
            w.setStatus(ResourceStatus.AVAILABLE);
            workers.save(w);
        }
        for (int i = 1; i <= 6; i++) {
            VehicleDocument v = new VehicleDocument();
            v.setType(i <= 4 ? VehicleType.TRACTOR : VehicleType.TRUCK);
            v.setRegistration("%d TUN %d".formatted(120 + i, 4500 + i));
            v.setCapacityKg(i <= 4 ? 3000 : 8000);
            v.setStatus(ResourceStatus.AVAILABLE);
            vehicles.save(v);
        }
        for (int i = 1; i <= 20; i++) {
            BinDocument b = new BinDocument();
            b.setCode("BIN-%03d".formatted(i));
            b.setCapacityKg(500);
            b.setStatus(ResourceStatus.AVAILABLE);
            bins.save(b);
        }
        saveEquipment(EquipmentType.NET, "Filet de récolte", 40);
        saveEquipment(EquipmentType.VIBRATOR, "Peigne vibrant", 12);
        saveEquipment(EquipmentType.OTHER, "Échelle aluminium", 25);

        // 4. Deux pressoirs autour de Sfax (§5.5 Press)
        mongo.getCollection("presses").insertMany(List.of(
                new Document("name", "Pressoir de la coopérative Sfax Nord")
                        .append("location", new Document("type", "Point").append("coordinates", List.of(10.75, 34.74)))
                        .append("dailyCapacityKg", 50000).append("availableCapacityKg", 32000),
                new Document("name", "Pressoir Sfax Sud - Agareb")
                        .append("location", new Document("type", "Point").append("coordinates", List.of(10.68, 34.71)))
                        .append("dailyCapacityKg", 40000).append("availableCapacityKg", 28000)
        ));

        log.info("Seed terminé : {} comptes, {} vergers, {} demandes.",
                users.count(), orchards.count(), requests.count());
    }

    private UserDocument saveUser(String name, String email, Role role, String hash) {
        UserDocument u = new UserDocument();
        u.setFullName(name); u.setEmail(email); u.setPasswordHash(hash);
        u.setRole(role); u.setActive(true);
        return users.save(u);
    }

    private void createOrchardsForFarmer(String farmerId, int idx, int count, Random rnd) {
        for (int k = 1; k <= count; k++) {
            double lon = Math.round((10.76 + (rnd.nextDouble() - 0.5) * 0.30) * 10000.0) / 10000.0;
            double lat = Math.round((34.73 + (rnd.nextDouble() - 0.5) * 0.25) * 10000.0) / 10000.0;
            OrchardDocument o = new OrchardDocument();
            o.setFarmerId(farmerId);
            o.setName(idx == 0 && k == 1 ? "North Orchard" : "Verger Sfax %d-%d".formatted(idx, k));
            o.setAreaHectares(Math.round((1.5 + rnd.nextDouble() * 8.0) * 10.0) / 10.0);
            o.setOliveVariety(VARIETIES[(idx + k) % VARIETIES.length]);
            o.setLocation(new GeoJsonPoint(lon, lat));   // [longitude, latitude]
            o.setMaturityStatus(MATURITIES[(idx + k) % MATURITIES.length]);
            o.setPublicVisibility(k % 2 == 1);
            o = orchards.save(o);

            if (k == 1) {
                HarvestRequestDocument hr = new HarvestRequestDocument();
                hr.setFarmerId(farmerId);
                hr.setOrchardId(o.getId());
                hr.setOrchardName(o.getName());
                hr.setType(idx % 3 == 0 ? HarvestType.URGENT : HarvestType.PLANNED);
                hr.setUrgencyReason(hr.getType() == HarvestType.URGENT ? "Maturité optimale" : null);
                hr.setStatus(HarvestStatus.SUBMITTED);
                hr.setRequestedAt(Instant.now());
                hr.setPreferredDate(LocalDate.now().plusDays(2 + (idx % 7)));
                hr.setEstimatedQuantityKg(BigDecimal.valueOf(1000 + (idx % 10) * 250L));
                requests.save(hr);
            }
        }
    }

    private void saveEquipment(EquipmentType type, String label, int qty) {
        EquipmentDocument e = new EquipmentDocument();
        e.setType(type); e.setLabel(label); e.setQuantity(qty); e.setStatus(ResourceStatus.AVAILABLE);
        equipment.save(e);
    }

    private int parseFarmersCount(ApplicationArguments args, List<String> raw) {
        if (args.containsOption("farmers")) {
            return Integer.parseInt(args.getOptionValues("farmers").get(0));
        }
        int i = raw.indexOf("--farmers");
        return (i >= 0 && i + 1 < raw.size()) ? Integer.parseInt(raw.get(i + 1)) : 20;
    }
}
