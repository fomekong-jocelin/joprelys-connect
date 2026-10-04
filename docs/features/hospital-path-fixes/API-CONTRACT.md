# Contrat API
GET /api/hospitalizations/placement-options : unités actives, catalogue, espaces d'hébergement actifs, affectations actives et projection praticiens (id/displayName/role/enabled/activeOrganizationalUnits). Permission HOSPITALIZATION_READ pour noms des responsables ; options de placement nécessitent HOSPITALIZATION_ADMIT ou HOSPITALIZATION_TRANSFER (contrôleurs séparés si nécessaire).
GET /api/hospitalizations/placement-beds?spaceId=UUID : liste des lits du tenant avec disponibilité opérationnelle ; HOSPITALIZATION_ADMIT ou HOSPITALIZATION_TRANSFER ; 404 hors scope.
GET /api/hospitalizations/admission-visits/{patientId} : projection id/visitNumber/reason/createdAt/status des visites du patient autorisé ; HOSPITALIZATION_ADMIT ; 404/403 patient non accessible. Ne retourne ni constantes ni consultation.
Aucun organizationId externe accepté. Médecins actifs affectés datés. Catalogue national non sensible. GET praticiens readonly limité à l'établissement pour les libellés de séjour.
GET /api/hospitalizations/practitioners : projection id/displayName/role/enabled/activeOrganizationalUnits, HOSPITALIZATION_READ. Inclut médecins et infirmiers pour les libellés historiques et le CRO ; les choix d'admission restent limités aux médecins actifs affectés à l'unité. Aucun email ou secret exposé.

GET /api/hospitalizations/{id}/eligible-medications : HOSPITALIZATION_MEDICATION_ADMINISTER ; séjour actif du tenant ; liste prescriptionItemId/medicationName/dosage/posology/route/prescriptionNumber. ACTIVE et non expirée ; patient canonique, tenant explicite.

BREAKING CHANGE : POST /api/hospitalizations/{id}/medication-administrations exige désormais prescriptionItemId (UUID, non null). Absence : 400 ; référence inadmissible ou nom divergent : 409 ; permission inchangée. Le nom enregistré est celui de la prescription. Dose textuelle et date conservent leur contrat. Les anciens clients sans référence doivent être adaptés avant déploiement coordonné.

Tous les autres endpoints d'écriture et de configuration gardent leurs permissions et payloads. Aucun droit d'administration de structure/personnel n'est accordé. Un soignant autorisé au transfert peut lire le placement ; l'accès aux visites d'admission exige séparément HOSPITALIZATION_ADMIT.
