# Bases de données H2

| Emplacement | Rôle | Git |
|---|---|---|
| `data/demo/helpdesk.mv.db` | Base de démonstration, données fictives uniquement | Versionnée (pas encore créée) |
| `data/runtime/helpdesk.mv.db` | Base de travail locale | Ignorée |

Au démarrage, `DemoDatabaseInstaller` copie la base de démonstration vers `data/runtime/` **seulement si** la base de travail n'existe pas. Une base existante n'est jamais écrasée. Sans base de démonstration, l'application crée une base vide.

## Repartir de la base de démonstration

Arrêter l'application, renommer ou déplacer `data/runtime/helpdesk.mv.db`, puis redémarrer.

## Produire la base de démonstration

1. Préparer les données dans l'application. Aucune donnée personnelle réelle, aucun secret.
2. Arrêter proprement l'application (Ctrl+C) : H2 verrouille le fichier tant qu'elle tourne.
3. Copier `data/runtime/helpdesk.mv.db` vers `data/demo/helpdesk.mv.db`.

Les fichiers `.trace.db` sont des journaux H2 et ne doivent jamais être versionnés.
