# Dictionnaire de Données — OliveConnect (Semaine 2)

> Fixe les colonnes CSV et clés JSON des imports avant l'implémentation de l'analyseur (A15/A16, Semaine 3).

| `ImportType` | Format | Colonnes / clés obligatoires | Règles de validation & clé d'idempotence (`externalId`) |
|---|---|---|---|
| `ORCHARDS` | CSV / JSON | `externalId, farmerEmail, name, areaHectares, oliveVariety, longitude, latitude, publicVisibility` | `areaHectares > 0`, `longitude ∈ [-180, 180]`, `latitude ∈ [-90, 90]`, `farmerEmail` existant ; upsert sur `externalId` |
| `WORKERS` | CSV / JSON | `externalId, fullName, phone, skills` (`PICKING\|TRACTOR`) | `fullName` non vide ; upsert sur `externalId` |
| `VEHICLES` | CSV / JSON | `externalId, type, registration, capacityKg` | `type ∈ {TRACTOR, TRUCK}`, `capacityKg > 0` ; upsert sur `externalId` |
| `PRESSES` | CSV / JSON | `externalId, name, longitude, latitude, dailyCapacityKg, availableCapacityKg` | `0 <= availableCapacityKg <= dailyCapacityKg` ; upsert sur `externalId` |
| `MATURITY_READINGS` | CSV / JSON | `externalId, orchardExternalId, maturityStatus, observedAt` | `maturityStatus ∈ MaturityStatus`, `observedAt` ISO-8601 UTC ; met aussi à jour `orchards.maturityStatus` |
| `WEATHER_OBSERVATIONS` | CSV / JSON | `externalId, longitude, latitude, temperatureC, rainfallMm, observedAt` | coordonnées valides, `rainfallMm >= 0` |
| `PRESS_CAPACITY` | CSV / JSON | `externalId, pressExternalId, availableCapacityKg` | `0 <= availableCapacityKg <= dailyCapacityKg` |
