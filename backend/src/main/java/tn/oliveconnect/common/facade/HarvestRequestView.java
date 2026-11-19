package tn.oliveconnect.common.facade;

import java.math.BigDecimal;
import tn.oliveconnect.common.enums.HarvestStatus;

public record HarvestRequestView(
        String id, String farmerId, String orchardId, String orchardName,
        BigDecimal estimatedQuantityKg, HarvestStatus status) {}
