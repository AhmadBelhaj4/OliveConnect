package tn.oliveconnect.press.service;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.TourStatus;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.facade.CacheInvalidator;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.press.document.*;
import tn.oliveconnect.press.dto.PressDtos.*;
import tn.oliveconnect.press.repository.*;
import tn.oliveconnect.resource.document.BinDocument;
import tn.oliveconnect.resource.repository.BinRepository;
import tn.oliveconnect.security.AuthUser;
import tn.oliveconnect.tour.document.TourDocument;
import tn.oliveconnect.tour.repository.TourRepository;

@Service
public class PressService {

    private final PressRepository presses;
    private final PressReceptionRepository receptions;
    private final TourRepository tours;
    private final BinRepository bins;
    private final CacheInvalidator cache;

    public PressService(PressRepository presses, PressReceptionRepository receptions,
                        TourRepository tours, BinRepository bins, CacheInvalidator cache) {
        this.presses = presses; this.receptions = receptions; this.tours = tours;
        this.bins = bins; this.cache = cache;
    }

    public PageResponse<PressResponse> listPresses(Pageable pageable) {
        return PageResponse.from(presses.findAll(pageable), PressResponse::from);
    }

    public PressResponse updateCapacity(String id, PressCapacityPatchRequest req) {
        PressDocument p = presses.findById(id).orElseThrow(() -> ApiException.notFound("Pressoir introuvable."));
        if (req.availableCapacityKg() > p.getDailyCapacityKg()) {
            throw ApiException.validation("availableCapacityKg", "Ne peut pas dépasser la capacité journalière");
        }
        p.setAvailableCapacityKg(req.availableCapacityKg());
        return PressResponse.from(presses.save(p));
    }

    public PressReceptionResponse recordReception(AuthUser me, PressReceptionCreateRequest req) {
        TourDocument tour = tours.findById(req.tourId()).orElseThrow(() -> ApiException.notFound("Tournée introuvable."));
        if (tour.getStatus() != TourStatus.IN_PROGRESS && tour.getStatus() != TourStatus.COMPLETED) {
            throw ApiException.invalidTransition("La tournée doit être IN_PROGRESS ou COMPLETED pour une réception.");
        }
        BinDocument bin = bins.findById(req.binId()).orElseThrow(() -> ApiException.notFound("Benne introuvable."));
        PressDocument press = presses.findById(req.pressId()).orElseThrow(() -> ApiException.notFound("Pressoir introuvable."));

        // §5.5 : weightKg > 0 et <= capacité de la benne
        if (req.weightKg().compareTo(BigDecimal.valueOf(bin.getCapacityKg())) > 0) {
            throw ApiException.validation("weightKg",
                    "Dépasse la capacité de la benne (" + bin.getCapacityKg() + " kg)");
        }
        if (receptions.existsByTourIdAndBinId(req.tourId(), req.binId())) {
            throw ApiException.duplicate("binId", "Réception déjà enregistrée pour cette tournée et cette benne.");
        }

        PressReceptionDocument r = new PressReceptionDocument();
        r.setTourId(tour.getId());
        r.setBinId(bin.getId());
        r.setPressId(press.getId());
        r.setReceivedAt(req.receivedAt() != null ? req.receivedAt() : Instant.now());
        r.setWeightKg(req.weightKg());
        r.setQualityGrade(req.qualityGrade());
        r.setNotes(req.notes());
        r.setRecordedBy(me.id());   // depuis le token JWT

        try {
            r = receptions.save(r);
        } catch (DuplicateKeyException e) {
            throw ApiException.duplicate("binId", "Réception déjà enregistrée pour (tourId, binId).");
        }
        cache.dashboardChanged();
        return PressReceptionResponse.from(r);
    }

    public PageResponse<PressReceptionResponse> listReceptions(Pageable pageable) {
        return PageResponse.from(receptions.findAll(pageable), PressReceptionResponse::from);
    }
}
