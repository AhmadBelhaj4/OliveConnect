package tn.oliveconnect.common.facade;

public interface CacheInvalidator {
    void orchardChanged(String orchardId);
    void mapChanged();
    void dashboardChanged();
}
