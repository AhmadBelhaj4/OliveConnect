package tn.oliveconnect.common.facade;

import java.math.BigDecimal;
import java.util.List;

public interface HarvestRequestFacade {
    List<HarvestRequestView> findApproved(List<String> ids);
    void markScheduled(List<String> ids, String tourId);
    void markInProgress(List<String> ids);
    void markCompleted(String id, BigDecimal actualKg);
    void revertToApproved(List<String> ids);
}
