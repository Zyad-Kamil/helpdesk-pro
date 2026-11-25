# État du projet

Dernière mise à jour : 25/11/2026, mission 03.

## Exigences du cahier des charges

| Exigence | État |
|---|---|
| Spring Boot, MVC, Data JPA, Thymeleaf | Socle installé ; MVC et Thymeleaf pas encore utilisés |
| H2 embarquée | Implémenté (fichier) |
| H2 présente dans le repository | À faire : `data/demo/helpdesk.mv.db` |
| Cinq entités JPA minimum | Implémenté |
| OneToOne, OneToMany/ManyToOne, ManyToMany | Implémenté au niveau JPA, testé |
| Association et dissociation graphiques 1-N et N-N | Logique de service implémentée et testée ; écrans à faire |
| CRUD | Couche Service implémentée et testée ; écrans et API à faire |
| Recherche | Couche Service implémentée et testée ; écrans et API à faire |
| Logique métier | Implémenté et testé (statuts, affectations, délais de résolution) |
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
| 03 | Couche Service, DTO, règles métier, délais de résolution | Réalisée, en attente de validation |

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
| Couche Service | Les services renvoient des DTO (jamais d'entité) construits dans la transaction ; messages d'erreur en français ; règles détaillées dans [architecture.md](architecture.md) |
| Délais de résolution | LOW 72 h, MEDIUM 48 h, HIGH 24 h, CRITICAL 4 h, en heures calendaires ; horloge `Clock` injectable |
| Interprétations à valider | Réaffectation refusée dès que le ticket est `RESOLVED` ; catégorie et tags modifiables sur un ticket résolu, pas sur un ticket clôturé ; activation d'un technicien toujours permise |

Les choix de mapping sont détaillés dans [architecture.md](architecture.md).

## Risques

| Risque | Parade |
|---|---|
| Défaut H2 sur les CHECK | Corrigé par H2 2.5.252 ; test de non-régression conservé |
| `ddl-auto=update` ne renomme ni ne supprime de colonne | Régénérer la base de démonstration si le modèle change |
| Unicité sensible à la casse dans la base (catégorie, tag) | Contrôlée sans casse par les services ; la base seule n'empêche pas deux créations simultanées. E-mails stockés en minuscules |
| Accès de l'évaluateur au dépôt GitHub non vérifié | À confirmer par la direction |
| Commits authentiques de chaque membre | Organisation à définir par l'équipe |
| `develop` sans branche distante connue | Push à autoriser |

## Tâches restantes

1. Contrôleurs MVC et REST : traduction des exceptions métier en réponses HTTP (404, 409, 422) et en messages de formulaire.
2. Écrans CRUD, recherche, association et dissociation, tableau de bord (tickets en retard).
3. API REST et collection Bruno (les DTO existent).
4. Base de démonstration versionnée, données fictives.
5. Documentation finale.
