# FUNCTIONAL-SPEC — Disponibilités médecins et prise de rendez-vous patient

> Statut : **initiale** (Documentation First — démarrage EPIC-0025). Évolue avec le code.
> Ticket : `docs/ai/tickets/EPIC-0025-doctor-availability-appointments.md`

## 1. Problème métier

Aujourd'hui, Joprelys Connect ne gère que des visites « walk-in » (file d'attente à l'arrivée). Les patients ne peuvent ni consulter les disponibilités des médecins ni réserver à l'avance ; l'accueil n'a pas de cahier de rendez-vous (pourtant prévu par le Cahier des charges V2).

## 2. Utilisateurs concernés

| Profil | Besoin |
|---|---|
| `PATIENT` (portail web) | Consulter les médecins et leurs créneaux libres, réserver, consulter et annuler ses rendez-vous |
| `MEDECIN` | Définir et maintenir ses disponibilités récurrentes et ses indisponibilités |
| `AGENT_ACCUEIL` | Tenir le cahier de rendez-vous, réserver pour un patient, enregistrer l'arrivée (check-in) |
| `ADMIN_CLINIQUE` | Superviser l'agenda de la clinique et gérer les disponibilités de tous les médecins |

## 3. Objectif

Offrir un parcours complet : le médecin publie ses disponibilités → le patient réserve un créneau depuis le portail → l'accueil suit l'agenda et convertit le rendez-vous honoré en visite.

## 4. Périmètre

### Inclus (MVP)

- Disponibilités hebdomadaires récurrentes par médecin (jour de semaine, plage horaire, durée de créneau).
- Exceptions d'indisponibilité (congés, absences ponctuelles).
- Génération des créneaux réservables côté backend (le backend est maître de la vérité).
- Annuaire des médecins consultable par le patient (nom, spécialité, service).
- Réservation, consultation et annulation de RDV par le patient (portail existant).
- Cahier de rendez-vous accueil : agenda, réservation pour un patient, check-in.
- Conversion d'un RDV honoré en visite (lien avec la file d'attente existante).
- E-mails transactionnels : confirmation, annulation, rappel J-1.

### Exclu (versions ultérieures)

- Téléconsultation, SMS, paiement en ligne, liste d'attente, récurrences mensuelles.
- Synchronisation calendriers externes (Google/Outlook/ICS).
- Réservation inter-cliniques, application mobile Flutter.

## 5. Parcours utilisateur attendus

1. **Médecin** : ouvre « Mes disponibilités » → définit ex. lundi 08:00–12:00 (créneaux 30 min) → ajoute une absence le 24/07 → les créneaux correspondants disparaissent de la réservation.
2. **Patient** : portail → « Prendre rendez-vous » → choisit un médecin (filtre spécialité/service) → choisit une date → choisit un créneau libre → confirme → reçoit un e-mail de confirmation → retrouve le RDV dans « Mes rendez-vous » → peut l'annuler dans le délai autorisé.
3. **Agent d'accueil** : ouvre le cahier du jour → voit les RDV par médecin → à l'arrivée du patient, « check-in » → une visite est créée dans la file d'attente existante → le RDV passe à l'état honoré.

## 6. Règles métier (initiales, à affiner en STORY-2601)

- RM-01 : un créneau = durée configurable par médecin (défaut 30 min), aligné sur les plages publiées.
- RM-02 : double réservation impossible — un médecin a au plus un RDV actif par date/heure.
- RM-03 : réservation uniquement dans le futur, dans un horizon configurable (défaut 30 jours).
- RM-04 : un patient a au plus un RDV actif par médecin et par jour.
- RM-05 : annulation patient possible jusqu'à 24 h avant (configurable) ; ensuite seule la clinique annule.
- RM-06 : statuts : `CONFIRMED`, `CANCELLED_BY_PATIENT`, `CANCELLED_BY_CLINIC`, `COMPLETED`, `NO_SHOW`.
- RM-07 : la modification/suppression d'une disponibilité ne supprime jamais un RDV existant silencieusement ; tout conflit est tranché par une action explicite de la clinique (annulation + notification).
- RM-08 : le check-in d'un RDV crée une visite liée (`visit_id`) ; le RDV passe `COMPLETED` à la clôture de la visite.
- RM-09 : isolation multi-tenant stricte : un patient ne voit que les médecins/RDV de sa clinique.
- RM-10 : toutes les actions (réservation, annulation, check-in) sont auditées, sans PII dans les logs.

## 7. Critères d'acceptation (synthèse epic)

- Un médecin publie ses disponibilités et elles produisent exactement les créneaux attendus (exceptions déduites).
- Un patient réserve un créneau libre ; un second patient ne peut plus le réserver (test de concurrence).
- L'annulation respecte RM-05 et déclenche l'e-mail correspondant.
- L'agent d'accueil réalise le check-in et la visite apparaît dans la file d'attente existante.
- UI : textes 100 % i18n FR/EN, light/dark, arrondis ≤ 8 px, composants partagés, aucun Angular Material, aucun composant > 500 lignes.

## 8. Cas limites connus

- Deux patients réservent le même créneau simultanément → un seul succès, l'autre reçoit une erreur « créneau indisponible ».
- Changement de disponibilité avec RDV déjà réservés → RM-07.
- Rendez-vous le jour du changement d'heure / fuseau horaire de la clinique.
- Patient qui réserve puis tente d'annuler après le délai.
- No-show : le RDV reste traçable et comptabilisable.

## 9. Hypothèses et zones à clarifier

- Mono-fuseau par clinique en V1 (le fuseau est celui de la clinique).
- Les données RDV sont couvertes par le cadre de protection des données déjà en place (pas de flux nouveau vers un tiers) — à confirmer par le DPO si rappel e-mail contient des détails cliniques (recommandation : contenu minimal, sans motif détaillé).
- Le motif de RDV est un texte court libre ou une liste configurable — à trancher en STORY-2601 avec le référent clinique.
- Pas de paiement ni d'acompte à la réservation en V1.

## 10. Cloisonnement patient / professionnel — correctif P0 du 2026-07-18

- Le mode `PATIENT` est exclusif : aucun menu, route ou rôle professionnel ne doit être combiné à la session patient.
- Une permission RBAC chargée pendant une ancienne session professionnelle ne doit jamais influencer le portail patient.
- Un patient utilise uniquement `/patient/**` et `/api/patient/**` pour les rendez-vous ; `/clinic/availability` et `/api/availabilities/**` exigent strictement `AVAILABILITY_MANAGE`.
- Critère de non-régression : après le parcours médecin → déconnexion → patient, le menu ne contient que les entrées patient et une URL clinique directe est refusée.

## 11. Ergonomie Mobile-First & Résilience du Calendrier (Correctif P0 du 2026-07-28)

- **Vue Mobile-First (< 768px)** : Affichage d'un bandeau d'onglets jour par jour (Lun → Dim) avec dates claires, évitant le défilement horizontal. La sélection d'un jour affiche les plages configurées sous forme de cartes d'action et permet l'ajout rapide de créneaux sans forcer l'usage du calendrier 7 colonnes.
- **Grille Desktop (≥ 768px)** : Préservation de la grille hebdomadaire 7 colonnes type agenda.
- **Synchronisation strict jour / date** : La sélection d'un jour ou d'un créneau dans l'agenda alimente et synchronise automatiquement le jour de la semaine (`weekday`) et la date de début de validité (`validFrom`), sans retour intempestif à « Lundi ».
- **Résilience API** : Acceptation des requêtes HTTP `/api/availabilities` et `/api/availabilities/` pour parer aux normalisations d'URL de reverse proxy/Nginx.

