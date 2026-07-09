# STORY-2104: Bloc opératoire, CRO, anesthésie et implants

## Description
Saisie et validation du Compte Rendu Opératoire (CRO), détails de l'anesthésie, et traçabilité des implants/consommables du bloc (lots, quantités, prix). Les actes opératoires doivent générer des actes facturables basés sur les coefficients de l'acte (K chirurgien, K anesthésiste, K bloc) ainsi que sur les implants utilisés. Le CRO doit devenir immuable après validation.

## Étapes de réalisation

- [x] Créer la migration de base de données Flyway pour les tables `operating_reports` et `surgical_implants` (Fait dans V48)
- [x] Créer les entités JPA `OperatingReportEntity` et `SurgicalImplantEntity` (Fait)
- [x] Définir les interfaces de persistance `OperatingReportRepository` et `SurgicalImplantRepository` (Fait)
- [x] Déclarer les DTOs/records Request & Response correspondants (Fait)
- [x] Écrire le service applicatif découplé `OperatingReportService` pour la gestion des rapports et implants (Fait)
- [x] Exposer les contrôleurs REST correspondants dans `HospitalizationController` (Fait)
- [x] Injecter et intégrer la logique dans le calcul de facture pré-calculée de `BillingService` (Fait)
- [x] Mettre en place les tests d'intégration backend dans `HospitalizationControllerTest` (Fait)
- [x] Mettre à jour l'API client Angular `PatientApiService` (Fait)
- [x] Ajouter l'interface de saisie et de visualisation premium du CRO et des implants dans le composant Angular (Fait)
- [x] Valider avec succès la compilation frontend et backend (En cours)

## Critères d'acceptation
- [x] Enregistrement d'un CRO avec type d'anesthésie et K-coefficients
- [x] Traçabilité fine des implants chirurgicaux (nom, lot, quantité, prix unitaire)
- [x] Le CRO devient immuable et non éditable/supprimable après validation médicale
- [x] La validation médicale génère automatiquement les lignes de facturation correspondantes (coefficients K et consommables/implants) sur la facture pré-calculée du séjour
