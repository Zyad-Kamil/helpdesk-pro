# État du projet

Dernière mise à jour : 25/11/2026, mission 02.1.

## Exigences du cahier des charges

| Exigence | État |
|---|---|
| Spring Boot, MVC, Data JPA, Thymeleaf | Socle installé ; MVC et Thymeleaf pas encore utilisés |
| H2 embarquée | Implémenté (fichier) |
| H2 présente dans le repository | À faire : `data/demo/helpdesk.mv.db` |
| Cinq entités JPA minimum | Implémenté |
| OneToOne, OneToMany/ManyToOne, ManyToMany | Implémenté au niveau JPA, testé |
| Association et dissociation graphiques 1-N et N-N | À faire (méthodes d'entité prêtes) |
| CRUD | À faire (repositories prêts) |
| Recherche | À faire |
| Logique métier | À faire |
| API REST CRUD complète | À faire |
| Tests Bruno | À faire |
| Interface Bootstrap | À faire |
| Commits réguliers de chaque membre | À faire : un seul commit à ce jour |

## Progression

| Mission | Contenu | État |
|---|---|---|
| 01 | Audit technique initial | Validée avec réserves |
| 02 | Initialisation Spring Boot et modèle de données | Réalisée, en attente de validation |
| 02.1 | Correction de régression H2 (2.5.252, CHECK rétablies) | Réalisée, en attente de validation |

## Décisions

| Sujet | Décision |
|---|---|
| Versions | Spring Boot 4.1.3, Java 21 (direction) |
| Console H2 | Écartée : `spring-boot-h2console`, ajoutée par Initializr, est hors liste autorisée |
| Base de démonstration | Copiée au démarrage si la base de travail est absente, sans écrasement |
| Schéma | `ddl-auto=update` |
| Contraintes CHECK | Conservées, dialecte H2 standard ; H2 fixée à 2.5.252 (correctif de l'issue 4302) |
| Validation du modèle | Obligatoires : titre, description, demandeur et e-mail du ticket ; nom, prénom et e-mail du technicien ; nom de catégorie et de tag. Les champs du profil sont facultatifs |
| Valeurs par défaut | Ticket `OPEN` et `MEDIUM`, technicien actif |

Les choix de mapping sont détaillés dans [architecture.md](architecture.md).

## Risques

| Risque | Parade |
|---|---|
| Défaut H2 sur les CHECK | Corrigé par H2 2.5.252 ; test de non-régression conservé |
| `ddl-auto=update` ne renomme ni ne supprime de colonne | Régénérer la base de démonstration si le modèle change |
| Unicité sensible à la casse (e-mail, catégorie, tag) | Normaliser dans la couche Service |
| Accès de l'évaluateur au dépôt GitHub non vérifié | À confirmer par la direction |
| Commits authentiques de chaque membre | Organisation à définir par l'équipe |
| `develop` sans branche distante connue | Push à autoriser |

## Tâches restantes

1. Couche Service, règles métier et exceptions.
2. Écrans CRUD, recherche, association et dissociation, tableau de bord.
3. API REST, DTO et collection Bruno.
4. Base de démonstration versionnée, données fictives.
5. Documentation finale.
