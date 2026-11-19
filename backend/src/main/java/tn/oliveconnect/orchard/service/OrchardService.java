package tn.oliveconnect.orchard.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.MaturityStatus;
import tn.oliveconnect.common.enums.Role;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.facade.CacheInvalidator;
import tn.oliveconnect.common.web.PageResponse;
import tn.oliveconnect.orchard.document.OrchardDocument;
import tn.oliveconnect.orchard.dto.*;
import tn.oliveconnect.orchard.repository.OrchardRepository;
import tn.oliveconnect.security.AuthUser;

@Service
public class OrchardService {

    private static final String NOT_FOUND = "Verger introuvable.";
    private final OrchardRepository orchards;
    private final CacheInvalidator cache;

    public OrchardService(OrchardRepository orchards, CacheInvalidator cache) {
        this.orchards = orchards;
        this.cache = cache;
    }

    public OrchardResponse create(AuthUser me, OrchardCreateRequest req) {
        OrchardDocument o = new OrchardDocument();
        o.setFarmerId(me.id());
        o.setName(req.name().trim());
        o.setAreaHectares(req.areaHectares());
        o.setOliveVariety(req.oliveVariety().trim());
        o.setLocation(req.location().toGeoJson("location"));
        o.setMaturityStatus(MaturityStatus.UNKNOWN);
        o.setPublicVisibility(req.publicVisibility());
        o = orchards.save(o);
        cache.orchardChanged(o.getId());
        cache.mapChanged();
        return OrchardResponse.from(o);
    }

    public PageResponse<OrchardResponse> mine(AuthUser me, Pageable pageable) {
        return PageResponse.from(orchards.findByFarmerId(me.id(), pageable), OrchardResponse::from);
    }

    public OrchardResponse get(AuthUser me, String id) {
        return OrchardResponse.from(loadReadable(me, id));
    }

    public OrchardResponse patch(AuthUser me, String id, OrchardPatchRequest req) {
        OrchardDocument o = loadReadable(me, id);
        if (req.maturityStatus() != null && me.hasRole(Role.FARMER)) {
            throw ApiException.accessDenied("Seul le responsable de collecte peut modifier la maturité.");
        }
        if (req.name() != null) o.setName(req.name().trim());
        if (req.areaHectares() != null) o.setAreaHectares(req.areaHectares());
        if (req.oliveVariety() != null) o.setOliveVariety(req.oliveVariety().trim());
        if (req.location() != null) o.setLocation(req.location().toGeoJson("location"));
        if (req.publicVisibility() != null) o.setPublicVisibility(req.publicVisibility());
        if (req.maturityStatus() != null) o.setMaturityStatus(req.maturityStatus());
        o = orchards.save(o);
        cache.orchardChanged(o.getId());
        cache.mapChanged();
        return OrchardResponse.from(o);
    }

    private OrchardDocument loadReadable(AuthUser me, String id) {
        OrchardDocument o = orchards.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        if (me.isStaff()) return o;
        if (me.hasRole(Role.FARMER) && me.id().equals(o.getFarmerId())) return o;
        throw ApiException.notFound(NOT_FOUND);
    }
}
