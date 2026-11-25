# CLAUDE.md

Instructions permanentes du projet HelpDesk Pro. L'avancement est dans `docs/PROJECT_STATE.md`, l'architecture dans `docs/architecture.md`.

## Commandes

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test "-Dtest=EntityRelationshipTests"
```

`JAVA_HOME` doit pointer sur un JDK 21. Si nécessaire, le définir seulement dans le processus courant, jamais dans le système.

## Stack figée

Java 21, Spring Boot 4.1.3, Maven Wrapper `only-script`, H2, Thymeleaf, Bootstrap 5. Les imports et starters de Spring Boot 3.x ne sont pas réutilisables tels quels, par exemple :

- `spring-boot-starter-webmvc` remplace `spring-boot-starter-web` ;
- `DataJpaTest` est dans `org.springframework.boot.data.jpa.test.autoconfigure`.

Exclus : Lombok, Docker, React, JWT, microservices, dépendances superflues. Ne pas ajouter de dépendance sans validation.

## Conventions

- Code et Javadoc en anglais ; interface et documentation en français.
- Indentation par tabulations, injection par constructeur dans le code de production.
- Dépendances : `controller` → `service` → `repository` → `entity`. Pas de logique métier dans les contrôleurs, pas d'entité exposée en REST (passer par `dto`).
- Pas de classe créée seulement pour remplir un package.

## Modèle JPA

- Exactement cinq entités : `Ticket`, `Technician`, `TechnicianProfile`, `Category`, `Tag`.
- Toujours modifier les relations par les méthodes de synchronisation (`assignTechnician`, `addTag`, `assignProfile`…). Les getters de collections sont en lecture seule.
- Ces méthodes chargent des collections paresseuses : les appeler dans une transaction (`spring.jpa.open-in-view=false`).
- Jamais de `CascadeType.REMOVE` ni `ALL` sur une relation ManyToMany.
- `equals` sur l'identifiant, `hashCode` constant, `toString` sans relation ni donnée personnelle.
- Enums : `@Enumerated(STRING)` avec `@JdbcTypeCode(SqlTypes.VARCHAR)`, sinon Hibernate crée un `ENUM` H2 natif.
- Les contraintes `CHECK` des enums sont conservées (dialecte H2 standard) ; H2 est fixée à 2.5.252 car 2.4.240 a un défaut (issue 4302). Ne pas rétrograder H2.

## Base de données

- `ddl-auto=update` pour la base persistante, jamais `create` ni `create-drop`. `update` ne renomme et ne supprime aucune colonne.
- Les tests utilisent une base isolée grâce à `src/test/resources/config/application.properties`.
- Aucune donnée personnelle réelle, aucun secret. Voir `data/README.md`.

## Git

- Branche de travail : `develop`. Aucun commit, push, merge ou changement de branche sans autorisation de la direction.
- Commits authentiques de chaque membre, sous sa propre identité.
- Premier commit du wrapper : `git add --chmod=+x mvnw`.
- Aucun test désactivé ou contourné ; `clean verify` doit réussir avant toute livraison.
