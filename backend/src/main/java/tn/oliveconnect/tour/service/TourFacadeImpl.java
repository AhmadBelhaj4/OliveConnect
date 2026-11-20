package tn.oliveconnect.tour.service;

import org.springframework.stereotype.Service;
import tn.oliveconnect.common.enums.EntityType;
import tn.oliveconnect.common.enums.NotificationType;
import tn.oliveconnect.common.enums.TourStatus;
import tn.oliveconnect.common.facade.EntityRef;
import tn.oliveconnect.common.facade.NotificationFacade;
import tn.oliveconnect.common.facade.TourFacade;
import tn.oliveconnect.tour.repository.TourRepository;

@Service
public class TourFacadeImpl implements TourFacade {

    private final TourRepository tours;
    private final NotificationFacade notifications;

    public TourFacadeImpl(TourRepository tours, NotificationFacade notifications) {
        this.tours = tours; this.notifications = notifications;
    }

    @Override
    public void removeStop(String harvestRequestId) {
        tours.findByStopsHarvestRequestIdAndStatusNot(harvestRequestId, TourStatus.CANCELLED).ifPresent(tour -> {
            tour.getStops().stream()
                    .filter(s -> harvestRequestId.equals(s.getHarvestRequestId()))
                    .findFirst()
                    .ifPresent(removed -> {
                        tour.getStops().remove(removed);
                        tours.save(tour);
                        notifications.notify(removed.getFarmerId(), NotificationType.TOUR_CHANGED,
                                "Votre arrêt sur la tournée du %s a été retiré suite à l'annulation.".formatted(tour.getTourDate()),
                                new EntityRef(EntityType.TOUR, tour.getId()));
                    });
        });
    }
}
