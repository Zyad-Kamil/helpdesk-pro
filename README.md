# helpdesk-pro
Professional IT help desk management application built with Spring Boot, Thymeleaf, JPA and H2.

HelpDesk Pro est une application de gestion des incidents informatiques, réalisée dans le cadre d'un projet universitaire JEE.

## État actuel

Le socle technique, le modèle de données, la couche Service (CRUD, recherche, règles métier, délais de résolution) et l'interface web (Spring MVC, Thymeleaf, Bootstrap 5) sont en place : `http://localhost:8080/` affiche le tableau de bord. **L'API REST et la collection Bruno ne sont pas encore développées.** Le suivi détaillé est dans [docs/PROJECT_STATE.md](docs/PROJECT_STATE.md).

## Stack

| Technologie | Version |
|---|---|
| Java | 21 LTS |
| Spring Boot (Web MVC, Data JPA, Thymeleaf, Validation) | 4.1.3 |
| Hibernate ORM | 7.4 |
| H2 | 2.5.252 |
| Maven (via Maven Wrapper) | 3.10.1 |

Bootstrap 5.3.3 et Bootstrap Icons 1.11.3 sont fournis localement (`src/main/resources/static/vendor/`, licences MIT incluses) : l'interface fonctionne sans Internet. Prévu : Bruno.

## Prérequis

Un JDK 21, avec `JAVA_HOME` défini. Maven n'a pas besoin d'être installé.

## Lancement

Depuis la racine du projet, sous Windows :

```powershell
.\mvnw.cmd spring-boot:run
```

Sous Linux ou macOS :

```sh
./mvnw spring-boot:run
```

L'application écoute sur le port 8080. Ctrl+C l'arrête proprement.

## Interface web

L'interface est entièrement en français, dotée d'une identité propre (thème sombre par défaut, thème clair mémorisé, palette de commandes Ctrl+K, tableau de bord Bento, Kanban) et responsive. Elle fonctionne hors ligne.

| Route | Contenu |
|---|---|
| `/` | Tableau de bord : compteurs réels, répartitions par statut et priorité, incidents récents, interventions urgentes |
| `/tickets` | Liste, recherche et filtres (statut, priorité, technicien, catégorie, tag, retards) |
| `/tickets/board` | Kanban : une colonne par statut, avec l'étape suivante autorisée sur chaque carte |
| `/tickets/new`, `/tickets/{id}`, `/tickets/{id}/edit` | Création, fiche détaillée (statut, solution, échéance, technicien, catégorie, tags), modification |
| `/technicians`, `/technicians/{id}` | Techniciens : CRUD, recherche, filtre actif/inactif, activation, tickets associés, profil |
| `/technicians/{id}/profile/new`, `/technicians/{id}/profile/edit` | Création et modification du profil |
| `/profiles` | Liste et recherche des profils |
| `/categories`, `/categories/{id}` | Catégories : CRUD, recherche, tickets associés (ajout et retrait) |
| `/tags`, `/tags/{id}` | Tags : CRUD, recherche, tickets associés (ajout et retrait) |

Toute modification passe par un `POST` suivi d'une redirection (Post/Redirect/Get) ; les suppressions demandent une confirmation. Les actions refusées par une règle métier renvoient à la page d'origine avec un message explicite.

## Tests et construction

```powershell
.\mvnw.cmd clean verify
```

```powershell
java -jar target\helpdesk-pro-0.0.1-SNAPSHOT.jar
```

Les tests utilisent une base isolée et ne touchent jamais à `data/`.

## Base de données

Les données sont enregistrées dans `data/runtime/`, ignoré par Git. Au premier démarrage, la base de démonstration `data/demo/helpdesk.mv.db` y est copiée si elle existe (elle n'est pas encore créée). Une base existante n'est jamais écrasée. Voir [data/README.md](data/README.md).

## Documentation

- [Architecture et modèle de données](docs/architecture.md)
- [État du projet](docs/PROJECT_STATE.md)
- [Conventions](CLAUDE.md)
