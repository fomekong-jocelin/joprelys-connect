# Functional Spec — Saisie & Consultation des Constantes Patient (Mobile Flutter)

## 1. Contexte & Besoin Métier
Dans l'application mobile Flutter pour praticiens et soignants, le professionnel doit pouvoir consulter et saisir rapidement les constantes vitales d'un patient en attente depuis la file active du tableau de bord.

## 2. Exigences Fonctionnelles
- **Déclenchement** : Cliquer sur une carte de visite patient dans la file active du tableau de bord (`_ActiveVisitCard`) ou sur son badge de constantes ouvre la feuille de saisie / consultation des constantes vitales.
- **Formulaire de constantes** :
  - Température (°C) [30.0 - 45.0]
  - Poids (kg) [1.0 - 500.0]
  - Taille (cm) [30 - 250]
  - Pouls (bpm) [20 - 250]
  - Tension Systolique (mmHg) [40 - 250]
  - Tension Diastolique (mmHg) [30 - 150]
  - Saturation SpO2 (%) [50 - 100]
  - Glycémie (g/L) [0.1 - 10.0]
  - Fréquence respiratoire (cycles/min) [5 - 100]
  - Échelle de douleur (EVA) [0 - 10]
- **Calcul automatique de l'IMC (BMI)** : Affiché dynamiquement lorsque le poids et la taille sont valides.
- **Modes & i18n** :
  - Validation client avant envoi.
  - Support multilingue FR/EN (`AppLocalizations`).
  - Support Dark & Light Mode avec respect strict des tokens Joprelys (arrondis 4–8px, pas de pilules).
- **Mise à jour à chaud** : Après enregistrement réussi (`POST /api/visits/{id}/vitals`), la file active est rafraîchie et la carte patient bascule à l'état "Constantes OK" (Prêt).
