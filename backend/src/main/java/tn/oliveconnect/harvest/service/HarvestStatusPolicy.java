package tn.oliveconnect.harvest.service;

import static tn.oliveconnect.common.enums.HarvestStatus.*;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.error.ApiException;

/**
 * Table des transitions autorisées d'une demande (plan §4).
 * La policy ne connaît pas l'acteur : ce sont les services qui vérifient QUI a le droit
 * (ex. SCHEDULED -> APPROVED n'est déclenché que par le système via la façade).
 */
@Component
public class HarvestStatusPolicy {

    private static final Map<HarvestStatus, Set<HarvestStatus>> ALLOWED = Map.of(
            SUBMITTED,   EnumSet.of(APPROVED, REJECTED, CANCELLED),
            APPROVED,    EnumSet.of(SCHEDULED, CANCELLED),
            SCHEDULED,   EnumSet.of(IN_PROGRESS, APPROVED, CANCELLED),
            IN_PROGRESS, EnumSet.of(COMPLETED),
            REJECTED,    EnumSet.noneOf(HarvestStatus.class),   // final
            COMPLETED,   EnumSet.noneOf(HarvestStatus.class),   // final
            CANCELLED,   EnumSet.noneOf(HarvestStatus.class));  // final

    public boolean canTransition(HarvestStatus from, HarvestStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public void assertTransition(HarvestStatus from, HarvestStatus to) {
        if (!canTransition(from, to)) {
            throw ApiException.invalidTransition("Transition " + from + " → " + to + " interdite.");
        }
    }

    /** PATCH autorisé seulement avant validation définitive (§6 : SUBMITTED, APPROVED). */
    public boolean isEditable(HarvestStatus status) {
        return status == SUBMITTED || status == APPROVED;
    }
}
