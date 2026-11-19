package tn.oliveconnect.common.facade;

/**
 * Facade commune figée pour l'intégration des modules (contrats-v1).
 * Tout changement sur cette interface doit faire l'objet d'une PR contract-change.
 */
public interface ContractFacade {
    String getVersion();
}
