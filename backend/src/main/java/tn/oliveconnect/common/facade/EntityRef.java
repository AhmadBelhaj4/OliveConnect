package tn.oliveconnect.common.facade;

import tn.oliveconnect.common.enums.EntityType;

public record EntityRef(EntityType entityType, String entityId) {}
