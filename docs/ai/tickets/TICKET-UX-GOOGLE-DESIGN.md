# TICKET-UX-GOOGLE-DESIGN — Refonte UI/UX : Navigation Contextuelle & Pages Dédiées

## 1. Objectif

Mettre en œuvre une architecture de navigation contextuelle dans la Sidebar (type Google Cloud Console) pour le Dossier Patient Unique (DPU), et éclater les onglets du portail Patient en pages dédiées avec menu de navigation complet, éliminant les écrans monolithiques avec onglets internes.

## 2. Critères d'acceptation

### A. Dossier Patient Clinique (Praticiens)
- [ ] Lorsqu'un patient est sélectionné (sur `/patients/:id/*`), la Sidebar affiche une section contextuelle avec le nom du patient en sous-titre ou en en-tête de section.
- [ ] Sous cet en-tête contextuel, la Sidebar présente les liens directs vers les sous-sections :
  - Fiche d'identité (`/patients/:id/profile`)
  - Dossier Médical (`/patients/:id/consultations`)
  - Analyses & Labo (`/patients/:id/lab-orders`)
  - Hospitalisations (`/patients/:id/hospitalizations`)
  - Journal d'Audit (`/patients/:id/audit-trail`)
- [ ] Le composant `PatientDetailComponent` n'affiche plus de barre d'onglets (tabs) interne. Il affiche seulement l'en-tête (Nom, DPU, actions de consultation/visite) et la zone `<router-outlet>`.
- [ ] Un service `ActivePatientService` permet de partager de manière réactive le patient chargé pour alimenter l'affichage de la Sidebar en temps réel.

### B. Portail Patient (Espace Patient)
- [ ] La Sidebar pour le rôle `PATIENT` contient les menus complets et distincts :
  - Accueil (`/patient/dashboard`)
  - Mes Ordonnances & Visites (`/patient/prescriptions`)
  - Partage & Consentements (`/patient/consents`)
  - Journal de Sécurité (`/patient/audit`)
  - Demandes de Cliniques (`/patient/requests`)
  - Notifications (`/patient/notifications`)
- [ ] Le tableau de bord principal (`/patient/dashboard`) affiche un écran de bienvenue premium, un résumé du profil de santé et des raccourcis.
- [ ] Les autres sections sont éclatées dans des pages dédiées reliées au routeur.
- [ ] Les traductions i18n correspondantes sont intégrées sans aucun texte codé en dur.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0012 — Refonte UI/UX Premium |
| User story parent | N/A |
| Sprint cible | SPRINT-0009 |
| Priorité business | P0 |
| Complexité | L |
| Story points | 8 |
| Profil recommandé | Senior / Lead |
| Effort estimé senior | 0.8j |
| Effort estimé intermédiaire | 1.3j |
| Effort estimé junior | 2.0j |
| Responsable | Frontend Agent |
| Reviewer obligatoire | Antigravity |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | STORY-1804 |
| Bloquants connus | Aucun |
