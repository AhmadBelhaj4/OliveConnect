package tn.oliveconnect.common.facade;

public interface TourFacade {
    /** Appelé par A quand une demande SCHEDULED est annulée : retire son arrêt de la tournée. */
    void removeStop(String harvestRequestId);
}
