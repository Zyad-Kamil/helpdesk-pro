# helpdesk-pro
Professional IT help desk management application built with Spring Boot, Thymeleaf, JPA and H2.

HelpDesk Pro est une application de gestion des incidents informatiques, réalisée dans le cadre d'un projet universitaire JEE.

## État actuel

Le socle technique, le modèle de données et la couche Service (CRUD, recherche, règles métier, délais de résolution) sont en place. **Les écrans et l'API REST ne sont pas encore développés** : `http://localhost:8080/` répond donc 404. Le suivi détaillé est dans [docs/PROJECT_STATE.md](docs/PROJECT_STATE.md).

## Stack

| Technologie | Version |
|---|---|
| Java | 21 LTS |
| Spring Boot (Web MVC, Data JPA, Thymeleaf, Validation) | 4.1.3 |
| Hibernate ORM | 7.4 |
| H2 | 2.5.252 |
| Maven (via Maven Wrapper) | 3.10.1 |

Prévus : Bootstrap 5 et Bruno.

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
