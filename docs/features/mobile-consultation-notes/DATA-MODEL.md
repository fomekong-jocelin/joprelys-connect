# Modèle de données — Diagnostic unique de consultation

## Modèle actif

La table `consultations` conserve un seul champ diagnostique métier :

| Colonne | Type | Règle |
|---|---|---|
| `diagnosis` | `TEXT NOT NULL` | Diagnostic documenté par le praticien ; le niveau de certitude reste exprimé dans le texte |

Les colonnes `suspected_diagnosis` et `final_diagnosis` ajoutées par V31 sont
supprimées par V109. Elles ne sont plus mappées par l'entité JPA et ne sont plus
exposées par l'API.

## Migration V109

Avant suppression des colonnes, V109 :

1. copie les valeurs d'origine dans
   `consultation_diagnosis_migration_archive` lorsqu'au moins une ancienne
   colonne contient une valeur ;
2. retient la première valeur non vide selon l'ordre
   `final_diagnosis` → `diagnosis` → `suspected_diagnosis` ;
3. supprime les deux colonnes redondantes de `consultations`.

L'archive est une trace technique de migration, n'est reliée à aucun endpoint et
est supprimée en cascade si la consultation source est supprimée. Elle contient
des données de santé et reste soumise aux mêmes règles d'accès, de sauvegarde et
de rétention que la base clinique.

## Rollback

Le rollback applicatif nécessite une migration forward dédiée : recréer les
colonnes, restaurer les valeurs archivées puis redéployer une version compatible.
La migration Flyway V109 ne doit jamais être modifiée après application.
