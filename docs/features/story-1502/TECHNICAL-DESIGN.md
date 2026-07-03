# TECHNICAL-DESIGN — Centre de notifications sur le portail patient (IHM) - STORY-1502

## 1. Objectif technique

Implémenter le composant Angular autonome `PatientNotificationsComponent`, l'intégrer au dashboard du portail patient sous la forme d'un onglet supplémentaire, ajouter les méthodes d'appels API au service de communication et rédiger des tests unitaires Vitest de l'IHM.

## 2. Stack concernée

- [x] Angular (Frontend)
- [x] Documentation

## 3. Contraintes projet obligatoires

- Tailwind CSS v4 obligatoire, pas d'Angular Material.
- Thème centralisé light/dark et internationalisation FR/EN respectés.
- Validation des tests unitaires.

## 4. Architecture cible

Le raccordement s'effectue comme suit :
```text
PatientPortalService (API Calls)
  └── getNotifications()
  └── markNotificationAsRead(id)
  └── markAllNotificationsAsRead()

PatientDashboardComponent (Main Dashboard)
  ├── Imports PatientNotificationsComponent
  └── Navigates to 'notifications' tab
```

## 5. Fichiers impactés ou créés

| Fichier | Type d'impact | Rôle |
|---|---|---|
| `web/src/app/patient/portal/services/patient-portal.service.ts` | Modification | Ajout des méthodes HTTP et du modèle `PatientNotification` |
| `web/src/app/patient/portal/components/patient-notifications.component.ts` | Nouveau | Composant autonome Angular pour la liste et les actions |
| `web/src/app/patient/portal/patient-dashboard.component.ts` | Modification | Intégration de l'onglet Notifications, affichage du badge |
| `web/src/app/core/i18n/i18n.service.ts` | Modification | Traduction complète des libellés (FR/EN) |
| `web/src/app/patient/portal/patient-portal.spec.ts` | Modification | Ajout des tests unitaires frontend pour le centre de notifications |
