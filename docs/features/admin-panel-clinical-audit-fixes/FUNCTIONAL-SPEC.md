# Panel administrateur — corrections de l'audit clinique

Source : QA-20261004-ADMIN-PANEL-CLINICAL-AUDIT ; ticket FIX-20261004-ADMIN-PANEL-CLINICAL-AUDIT.

Les comptes plateforme peuvent consulter et enregistrer leur profil sans affectation hospitalière. L'absence de contexte professionnel ne masque pas les données du profil ; une panne de ce contexte est signalée séparément.
Le dashboard donne accès au journal réellement disponible, sous ses permissions existantes, et propose des destinations distinctes pour établissements et interopérabilité.
Un service possède obligatoirement un code de discipline du catalogue et peut recevoir un intitulé local. Les anciens services sans intitulé restent lisibles avec leur libellé catalogue. Les responsables médicaux et infirmiers sont associés dans la gouvernance organisationnelle existante, avec historique et isolation établissement.
Les catalogues couvrent les disciplines et activités citées par l'audit. Néonatalogie et urgences admettent un profil hospitalier ; des espaces d'hôpital de jour, dialyse, chimiothérapie et chirurgie ambulatoire sont proposés.
Observation, rédaction de prescription, signature médicale, validation pharmaceutique et délivrance sont des actes autorisés séparément côté serveur. Un acte validé conserve son auteur et horodatage et ne peut être réécrit silencieusement. Aucun mécanisme n'est présenté comme une signature certifiée ou un horodatage qualifié sans fournisseur de confiance.
Le formulaire établissement indique les erreurs au champ, explique le type sans inventer de modules automatiquement activés et fonctionne en FR/EN. Breadcrumbs, téléversement et messages profil suivent la langue active.

Critères et cas limites : tâches ADMIN-01 à 05 du ticket ; anciens payloads/catalogues préservés, absence de contexte, erreurs 401/403/5xx, noms vides ou trop longs, profils non habilités, appels hors tenant, écritures après validation.

## Signature PNG — complément utilisateur du 2026-10-04

Le médecin téléverse depuis Mon profil une image PNG ou JPEG de sa signature ; elle est décodée et enregistrée réellement en PNG, y compris pour un JPEG ou un nom de fichier trompeur. Transparence existante conservée, proportions préservées et taille normalisée à 500 pixels sur le côté le plus long. L'image du médecin reste associée à son profil lors de l'enregistrement et affichée sur les comptes rendus/ordonnances par le générateur PDF existant. Une image illisible, non prise en charge ou aux dimensions excessives est refusée sans stockage brut. Le médecin actif peut associer la signature à son propre profil ; un administrateur habilité peut continuer à préparer les assets du médecin par la gestion du personnel. FILE_UPLOAD reste exigé et ne donne aucun droit de signer un acte. Logos/photos/cachets conservent leur circuit existant.
Ce circuit visuel utilise les composants gratuits/open source existants, sans service payant. Le scellement médical tracé reste distinct de l'image ; aucune conversion PNG n'est présentée comme preuve de signature qualifiée. DSS est retenu pour le futur volet cryptographique ; certificats/horodatage de confiance restent une dépendance séparée.
