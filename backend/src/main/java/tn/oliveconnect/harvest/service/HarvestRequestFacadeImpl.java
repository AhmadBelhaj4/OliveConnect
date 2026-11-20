package tn.oliveconnect.harvest.service;

import static tn.oliveconnect.common.enums.HarvestStatus.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.facade.CacheInvalidator;
import tn.oliveconnect.common.facade.HarvestRequestFacade;
import tn.oliveconnect.common.facade.HarvestRequestView;
import tn.oliveconnect.harvest.document.HarvestRequestDocument;
import tn.oliveconnect.harvest.repository.HarvestRequestRepository;

/**
 * Transitions déclenchées par le SYSTÈME (tournées de B). Règle : on valide TOUT avant d'écrire,
 * pour ne jamais laisser la moitié des demandes dans un nouvel état.
 */
@Service
public class HarvestRequestFacadeImpl implements HarvestRequestFacade {

    private final HarvestRequestRepository requests;
    private final HarvestStatusPolicy policy;
    private final CacheInvalidator cache;

    public HarvestRequestFacadeImpl(HarvestRequestRepository requests, HarvestStatusPolicy policy,
                                    CacheInvalidator cache) {
        this.requests = requests; this.policy = policy; this.cache = cache;
    }

    @Override
    public List<HarvestRequestView> findApproved(List<String> ids) {
        return requests.findAllById(ids).stream()
                .filter(r -> r.getStatus() == APPROVED)
                .map(r -> new HarvestRequestView(r.getId(), r.getFarmerId(), r.getOrchardId(),
                        r.getOrchardName(), r.getEstimatedQuantityKg(), r.getStatus()))
                .toList();
    }

    @Override
    public void markScheduled(List<String> ids, String tourId) {
        List<HarvestRequestDocument> docs = loadAll(ids);
        docs.forEach(d -> policy.assertTransition(d.getStatus(), SCHEDULED));
        Instant now = Instant.now();
        docs.forEach(d -> { d.setStatus(SCHEDULED); d.setScheduledTourId(tourId); d.setScheduledAt(now); });
        saveAndInvalidate(docs, false);
    }

    @Override
    public void markInProgress(List<String> ids) {
        List<HarvestRequestDocument> docs = loadAll(ids);
        docs.forEach(d -> policy.assertTransition(d.getStatus(), IN_PROGRESS));
        docs.forEach(d -> d.setStatus(IN_PROGRESS));
        saveAndInvalidate(docs, false);
    }

    @Override
    public void markCompleted(String id, BigDecimal actualKg) {
        List<HarvestRequestDocument> docs = loadAll(List.of(id));
        HarvestRequestDocument d = docs.get(0);
        policy.assertTransition(d.getStatus(), COMPLETED);
        d.setStatus(COMPLETED);
        d.setActualQuantityKg(actualKg);       // la source de la quantité réelle est l'arrêt de tournée (§5.5)
        d.setCompletedAt(Instant.now());
        saveAndInvalidate(docs, true);
    }

    @Override
    public void revertToApproved(List<String> ids) {
        List<HarvestRequestDocument> docs = loadAll(ids);
        docs.forEach(d -> policy.assertTransition(d.getStatus(), APPROVED));
        docs.forEach(d -> { d.setStatus(APPROVED); d.setScheduledTourId(null); d.setScheduledAt(null); });
        saveAndInvalidate(docs, false);
    }

    private List<HarvestRequestDocument> loadAll(List<String> ids) {
        List<HarvestRequestDocument> docs = requests.findAllById(ids);
        if (docs.size() != new HashSet<>(ids).size()) {
            throw ApiException.notFound("Demande introuvable.");
        }
        return docs;
    }

    private void saveAndInvalidate(List<HarvestRequestDocument> docs, boolean dashboard) {
        requests.saveAll(docs);
        docs.forEach(d -> cache.orchardChanged(d.getOrchardId()));
        cache.mapChanged();                 // harvestStatus affiché sur la carte
        if (dashboard) cache.dashboardChanged();
    }
}
