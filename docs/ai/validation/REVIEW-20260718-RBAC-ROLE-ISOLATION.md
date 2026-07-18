# Review checklist — Correctif RBAC inter-session

## Architecture et responsabilités

- ✅ Backend maître de l'autorisation ; Angular ne fait que masquer/refuser la navigation.
- ✅ Cache RBAC isolé dans `RbacApiService`; guard et navigation gardent une responsabilité distincte.
- ⚠️ Trois fichiers touchés dépassaient déjà 500 lignes (`PatientService`, `PatientDetailComponent`, `PatientConsultationsTabComponent`) ; la migration d'autorisation les réduit ou n'ajoute qu'un contrôle local, mais leur découpage reste suivi comme dette d'architecture hors correctif sécurité.
- ✅ Aucun changement DB, thème, texte visible ou contrat de payload ; la clé laboratoire existante est désormais obligatoire sans valeur par défaut.

## Sécurité

- ✅ Deny-by-default backend conservé.
- ✅ Permission `AVAILABILITY_MANAGE` exigée sur les sept endpoints concernés ; `AVAILABILITY_MANAGE_ALL` est nécessaire pour administrer un autre praticien.
- ✅ Session patient exclusive et claim mixte neutralisé.
- ✅ Réponses RBAC asynchrones d'une ancienne session ignorées.
- ✅ Déconnexion/changement d'identité : stockages, cookies accessibles, cache RBAC et patient actif purgés.
- ✅ Cookie HttpOnly expiré côté serveur et `Clear-Site-Data` ajouté aux frontières de session.
- ✅ Tests négatifs Angular ajoutés pour patient, claim mixte, rôle sans permission et changement entre rôles professionnels.
- ✅ Suite backend complète : 453 tests, 0 échec, 1 test ignoré.
- ✅ Test de politique empêchant `hasRole`/`hasAnyRole` dans les annotations des contrôleurs.
- ✅ Les élargissements de périmètre disponibilités, spatial et administration RBAC utilisent des authorities dédiées plutôt que des noms de rôles.
- ✅ Toutes les routes professionnelles gardées déclarent une permission ou l'entrée interne explicitement autorisée.

## Frontend et standards

- ✅ Angular 22, signaux et services existants conservés.
- ✅ Aucun Angular Material, Tailwind v3, token UI ou URL backend codée en dur ajouté.
- ✅ `proxy.conf.json` et configuration existante vérifiés.
- ✅ Tests Angular ciblés de purge/session, build production et contrôle i18n verts.
- ➖ Contrôle visuel light/dark : aucun changement de style ou de layout.

## Backend et standards

- ✅ Maven/pom.xml uniquement pour le changement ; aucun Gradle introduit.
- ✅ YAML uniquement ; suppression du secret laboratoire par défaut, variable d'environnement existante conservée.
- ✅ Controllers sans nouvelle logique métier.
- ✅ `.gitignore` racine et projets vérifiés, aucun secret ou artefact généré ajouté.

## Documentation et livraison

- ✅ Ticket, diagnostic, security review, ADR, specs, plan de tests, backlog, suivi, changelog, planning et risques mis à jour.
- ✅ Impact SemVer évalué PATCH ; `VERSION` inchangé car aucune release n'est préparée.
- ⚠️ Livraison finale soumise à la revue Tech Lead/RSSI/QA et à la recette métier multi-rôles.
