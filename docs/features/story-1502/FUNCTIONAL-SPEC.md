# FUNCTIONAL-SPEC — Centre de notifications sur le portail patient (IHM) - STORY-1502

## 1. Résumé métier

Le patient doit pouvoir suivre et gérer ses alertes de sécurité et ses informations médicales en temps réel depuis son espace. Pour cela, un centre de notifications interactif et esthétique est intégré au portail patient. Il affiche le décompte des messages non lus et permet de les consulter et de les marquer comme lus.

## 2. Objectifs

- Créer une interface utilisateur interactive (tiroir ou panneau) pour consulter ses notifications.
- Afficher un badge visible indiquant le nombre de notifications non lues.
- Fournir des actions pour marquer individuellement ou globalement comme lues.
- Présenter les alertes avec des styles de couleurs et d'icônes adaptés à leur gravité (sécurité, information, urgence).

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| Patient | Être informé des événements liés à son dossier et sa sécurité | Accède au centre de notifications de son espace. |

## 4. Périmètre

### Inclus

- Intégration du composant de liste de notifications (`PatientNotificationsComponent`) dans le tableau de bord.
- Affichage dynamique d'un badge rouge de décompte dans l'onglet/icône de navigation.
- Action "Tout marquer comme lu".
- Support linguistique (FR/EN) et thème sombre/clair.

### Exclus

- Notifications push par navigateur (Web Push API).

## 5. Parcours utilisateur

1. Le patient se connecte sur son espace personnel.
2. Un indicateur visuel (badge rouge avec le nombre, ex: `3`) apparaît à côté de l'onglet **Notifications**.
3. Il clique sur l'onglet **Notifications**.
4. La liste des notifications s'affiche, classées par date décroissante. Les notifications non lues ont un fond légèrement coloré.
5. Les alertes de sécurité (ex: Brise-Glace) sont marquées par une icône de bouclier rouge.
6. Il clique sur une notification ou sur le bouton "Tout marquer comme lu" :
   - Les notifications ciblées passent au statut lu et l'indicateur s'actualise.
