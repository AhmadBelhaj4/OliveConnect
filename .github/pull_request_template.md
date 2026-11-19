## Quoi
<!-- Tâche (ex. A5 Vergers) + résumé en 2 lignes -->

## Contrat
- [ ] Les réponses respectent docs/api-contract.md (mêmes clés, ni plus ni moins)
- [ ] Aucun champ / enum / code d'erreur renommé (sinon PR `contract-change`)

## Tests
- [ ] Cas nominal testé
- [ ] Au moins un cas d'erreur (400 / 401 / 403 / 404 / 409)
- [ ] `mvnw verify` ou `npm run build` vert

## Règles
- [ ] Pas de secret, pas de mot de passe / token dans les logs
- [ ] Controller → DTO (jamais un Document) ; farmerId depuis le token
- [ ] Docs du module à jour dans docs/

## Relecteur
<!-- A relit B, B relit C, C relit A -->
