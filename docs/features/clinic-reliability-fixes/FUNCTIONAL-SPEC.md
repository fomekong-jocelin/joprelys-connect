# Spécification fonctionnelle — Fiabilité de la création des cliniques

## Problème

La création d'un administrateur peut afficher un échec après avoir créé le compte si l'envoi du mot de passe échoue. Le logo téléversé pendant la création d'une clinique n'est pas conservé.

## Parcours attendu

1. Le super administrateur téléverse un logo puis crée une clinique.
2. La clinique retournée et les consultations suivantes contiennent le chemin du logo.
3. Il affecte un administrateur clinique.
4. Le compte n'est considéré créé que si le mot de passe temporaire a été transmis.
5. Si le service e-mail est indisponible, aucun compte partiel ne subsiste et l'interface invite à réessayer.

## Critères d'acceptation

- Conservation du logo à la création et à la modification.
- Aucun secret affiché dans l'interface ou la réponse API.
- Aucune création partielle lors d'un incident SMTP.
- Erreur utilisateur claire et non technique.
