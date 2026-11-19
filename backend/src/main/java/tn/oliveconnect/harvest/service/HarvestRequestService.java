package tn.oliveconnect.harvest.service;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.enums.HarvestType;
import tn.oliveconnect.common.enums.Role;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.facade.CacheInvalidator;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;
import tn.oliveconnect.harvest.dto.*;
import tn.oliveconnect.harvest.repository.*;
import tn.oliveconnect.orchard.document.OrchardDocument;
import tn.oliveconnect.orchard.repository.OrchardRepository;
import tn.oliveconnect.security.AuthUser;

@Service
public class HarvestRequestService {

    private static final String NOT_FOUND = "Demande introuvable.";
    private final HarvestRequestRepository requests;
    private final HarvestRequestSearchRepository search;
    private final OrchardRepository orchards;
    private final CacheInvalidator cache;

    public HarvestRequestService(HarvestRequestRepository requests, HarvestRequestSearchRepository search,
                                 OrchardRepository orchards, CacheInvalidator cache) {
        this.requests = requests;
        this.search = search;
        this.orchards = orchards;
        this.cache = cache;
    }

    public HarvestRequestResponse create(AuthUser me, HarvestRequestCreateRequest req) {
        if (req.type() == HarvestType.URGENT && (req.urgencyReason() == null || req.urgencyReason().isBlank())) {
            throw ApiException.validation("urgencyReason", "Obligatoire pour une demande urgente");
        }
        OrchardDocument orchard = orchards.findById(req.orchardId())
                .filter(o -> me.id().equals(o.getFarmerId()))
                .orElseThrow(() -> ApiException.notFound("Verger introuvable."));

        HarvestRequestDocument d = new HarvestRequestDocument();
        d.setFarmerId(me.id());
        d.setOrchardId(orchard.getId());
        d.setOrchardName(orchard.getName());
        d.setType(req.type());
        d.setUrgencyReason(req.type() == HarvestType.URGENT ? req.urgencyReason().trim() : null);
        d.setStatus(HarvestStatus.SUBMITTED);
        d.setRequestedAt(Instant.now());
        d.setPreferredDate(req.preferredDate());
        d.setEstimatedQuantityKg(req.estimatedQuantityKg());
        d = requests.save(d);
        cache.orchardChanged(orchard.getId());
        cache.mapChanged();
        return HarvestRequestResponse.from(d);
    }

    public PageResponse<HarvestRequestResponse> mine(AuthUser me, Pageable pageable) {
        return PageResponse.from(requests.findByFarmerId(me.id(), pageable), HarvestRequestResponse::from);
    }

    public PageResponse<HarvestRequestResponse> queue(HarvestStatus status, LocalDate from, LocalDate to, Pageable p) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.validation("from", "from doit être ≤ to");
        }
        return PageResponse.from(search.search(status, from, to, p), HarvestRequestResponse::from);
    }

    public HarvestRequestResponse get(AuthUser me, String id) {
        HarvestRequestDocument d = requests.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        boolean owner = me.hasRole(Role.FARMER) && me.id().equals(d.getFarmerId());
        if (!owner && !me.isStaff()) throw ApiException.notFound(NOT_FOUND);
        return HarvestRequestResponse.from(d);
    }
}
