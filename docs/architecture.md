# Architecture

## Vue d'ensemble

Application MVC monolithique avec couche Service. Les éléments en pointillés ne sont **pas encore développés**.

```mermaid
flowchart TD
    browser([Navigateur])
    bruno([Bruno])
    mvc["controller<br/>+ vues Thymeleaf"]
    rest["controller.rest"]
    service["service<br/>règles métier, transactions"]
    repository["repository<br/>Spring Data JPA"]
    entity["entity"]
    db[("H2 sur fichier<br/>data/runtime")]

    browser -.-> mvc
    bruno -.-> rest
    mvc --> service
    rest -.-> service
    service --> repository
    repository --> entity
    repository --> db

    classDef todo stroke-dasharray: 5 5
    class rest todo
```

| Package `com.helpdeskpro` | Responsabilité | État |
|---|---|---|
| `entity` | Entités JPA et enums | Implémenté |
| `repository` | Accès aux données, requêtes de recherche | Implémenté |
| `config` | Installation de la base de démonstration, horloge injectable (`Clock`) | Implémenté |
| `service` | Règles métier et transactions | Implémenté |
| `dto` | Requêtes validées (`*Request`) et réponses (`*Response`) | Implémenté |
| `exception` | Exceptions métier | Implémenté (traduction HTTP à faire) |
| `controller`, `controller.form` | Contrôleurs MVC, objets de formulaire, `WebAdvice` | Implémenté |
| `templates`, `static` | Vues Thymeleaf, CSS, JS, Bootstrap local | Implémenté |
| `controller.rest` | API REST | À développer |

Les packages à développer contiennent seulement un `package-info.java` décrivant leur rôle.

## Interface web

Spring MVC et Thymeleaf, avec du JavaScript natif uniquement (`static/js/theme.js`, `static/js/helpdesk.js`), sans bibliothèque ni outil de build. Les contrôleurs n'appellent que les services : ni repository, ni entité.

- **Pages.** Une page appelle `fragments/layout :: layout(titre, menu, contenu)` ; `fragments/components` fournit les messages flash, les badges de statut et de priorité (libellés dans `messages.properties`), l'échéance, les états vides et le formulaire de suppression.
- **Formulaires.** `controller.form` contient des beans modifiables qui conservent la saisie. Aucune validation n'y est dupliquée : le service valide la requête, et `FormSupport` rattache chaque violation au champ du même nom (messages en français, locale fixée à `fr`). Un doublon est rattaché au champ `email` ou `name`.
- **Post/Redirect/Get.** Toute modification est un `POST` suivi d'une redirection avec un message flash (`success` ou `error`). Aucun `GET` ne modifie de données.
- **Erreurs.** `WebAdvice` traduit `ResourceNotFoundException` en page 404 ; `BusinessRuleException`, `DuplicateResourceException` et `ConstraintViolationException` renvoient à la page d'origine (en-tête `Referer`, même hôte uniquement, sinon `/`) avec un message explicite. Les pages `error/404` et `error/error` couvrent les autres cas.
- **Actions proposées.** Les méthodes `editable()`, `technicianChangeable()`, `technicianRemovable()` et `nextStatus()` de `TicketResponse` (appuyées sur `TicketStatus.next()`) décident quels boutons sont affichés ; le service reste l'ultime garde.
- **Tableau de bord.** `DashboardService` calcule tout à partir des tickets et techniciens réels. Une intervention urgente est un ticket ouvert ou en cours, en retard ou de priorité critique (cinq au plus, échéance la plus proche d'abord).
- **Échappement.** Tout contenu utilisateur passe par `th:text` ou `th:field`, donc échappé.
- **Ressources.** Bootstrap 5.3.3 et Bootstrap Icons 1.11.3 sont dans `static/vendor/` avec leurs licences MIT ; le CSS du projet est centralisé dans `static/css/helpdesk.css`.

## Couche Service

Cinq services (`TicketService`, `TechnicianService`, `TechnicianProfileService`, `CategoryService`, `TagService`), transactionnels (`@Transactional`, lectures en `readOnly`) et validés (`@Validated`, `@Valid` sur les requêtes : `ConstraintViolationException` en cas de champ invalide). Ils reçoivent des `*Request` et renvoient des `*Response` : **aucune entité ne sort de la couche Service**, et les réponses sont entièrement chargées dans la transaction, donc sans `LazyInitializationException`. Les contrôleurs n'ont qu'à appeler un service.

### Exceptions

| Exception | Cas |
|---|---|
| `ResourceNotFoundException` | Identifiant inexistant |
| `DuplicateResourceException` | Nom de catégorie ou de tag, e-mail de technicien déjà pris (sans distinction de casse) |
| `BusinessRuleException` | Règle métier violée : transition interdite, suppression d'une entité encore liée, etc. |

Les messages sont en français et destinés à l'utilisateur. Leur traduction en réponses HTTP (404, 409, 422…) reste à écrire avec les contrôleurs.

### Règles du ticket

| Règle | Détail |
|---|---|
| Création | Toujours `OPEN` ; priorité `MEDIUM` par défaut. Statut, solution, technicien, catégorie et tags ne se règlent jamais par `create` ni `update` |
| Transitions | `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`, via `changeStatus`. Tout le reste est refusé |
| Passage en cours | Technicien actif obligatoire |
| Résolution | Solution non vide obligatoire ; `resolvedAt` renseigné par l'horloge |
| Ticket clôturé | Plus aucune modification (champs, statut, technicien, catégorie, tags). Seule la suppression reste possible |
| Affectation / réaffectation | Technicien actif uniquement, ticket `OPEN` ou `IN_PROGRESS` |
| Désaffectation | Ticket `OPEN` uniquement |
| Catégorie, tags | Modifiables tant que le ticket n'est pas clôturé |
| Suppression d'un ticket | Autorisée quel que soit le statut ; technicien, catégorie et tags sont conservés |
| Désactivation d'un technicien | Refusée s'il a un ticket `IN_PROGRESS` |
| Suppression d'un technicien, d'une catégorie, d'un tag | Refusée tant qu'un ticket y est lié ; le message indique le nombre de tickets. La suppression d'un technicien supprime son profil |

Un ticket résolu mais non clôturé peut encore être édité (champs descriptifs, catégorie, tags) ; seule la réaffectation est fermée.

### Délais de résolution

Échéance = `createdAt` + objectif de la priorité, en heures calendaires : `LOW` 72 h, `MEDIUM` 48 h, `HIGH` 24 h, `CRITICAL` 4 h (`TicketPriority.getResolutionTarget()`). Modifier la priorité déplace donc l'échéance.

- Ticket `OPEN` ou `IN_PROGRESS` : `overdue` vaut `true` si l'horloge dépasse strictement l'échéance ; `findOverdue()` les liste, le plus en retard d'abord.
- Ticket `RESOLVED` ou `CLOSED` : jamais en retard ; `resolvedWithinTarget` indique si `resolvedAt` ≤ échéance (`null` tant que le ticket est actif).
- L'horloge est un bean `Clock` (`ClockConfig`) ; les tests la remplacent par une horloge modifiable. `createdAt` et `updatedAt` restent posés par l'entité avec l'horloge système.

### Recherche

Insensible à la casse, par motif `LIKE` dont `%`, `_` et `!` sont échappés. Un mot-clé vide désactive le filtre.

| Service | Recherche |
|---|---|
| Ticket | Mot-clé (titre, description, demandeur, e-mail) + statut, priorité, technicien, catégorie, tag |
| Technicien | Mot-clé (prénom, nom, e-mail) + actif/inactif |
| Profil | Spécialisation, nom ou e-mail du technicien ; `findByTechnicianId` |
| Catégorie | Nom, description |
| Tag | Nom |

### Normalisation

Noms, titres et libellés : espaces de début et de fin retirés, espaces répétés réduits. E-mails : minuscules. Descriptions, solutions et biographies : seulement rognées (retours à la ligne conservés). Champs facultatifs vides : stockés à `null`.

## Modèle de données

```mermaid
classDiagram
    direction LR

    class Ticket {
        Long id
        String title
        String description
        String requesterName
        String requesterEmail
        TicketStatus status
        TicketPriority priority
        LocalDateTime createdAt
        LocalDateTime updatedAt
        LocalDateTime resolvedAt
        String resolution
    }
    class Technician {
        Long id
        String firstName
        String lastName
        String email
        boolean active
    }
    class TechnicianProfile {
        Long id
        String phone
        String specialization
        String biography
    }
    class Category {
        Long id
        String name
        String description
    }
    class Tag {
        Long id
        String name
    }
    class TicketStatus {
        <<enumeration>>
        OPEN
        IN_PROGRESS
        RESOLVED
        CLOSED
    }
    class TicketPriority {
        <<enumeration>>
        LOW
        MEDIUM
        HIGH
        CRITICAL
    }

    Technician "1" -- "0..1" TechnicianProfile : profile / technician
    Technician "0..1" -- "0..*" Ticket : technician / tickets
    Category "0..1" -- "0..*" Ticket : category / tickets
    Ticket "0..*" -- "0..*" Tag : tags / tickets
    Ticket ..> TicketStatus
    Ticket ..> TicketPriority
```

## Mapping des relations

| Relation | Propriétaire | Clé étrangère | Côté inverse | Cascade |
|---|---|---|---|---|
| Technician 1—0..1 TechnicianProfile | `TechnicianProfile.technician` | `technician_profile.technician_id`, `NOT NULL`, `UNIQUE` | `mappedBy = "technician"` | `ALL` + `orphanRemoval` depuis Technician |
| Technician 0..1—* Ticket | `Ticket.technician` | `ticket.technician_id`, nullable | `mappedBy = "technician"` | Aucune |
| Category 0..1—* Ticket | `Ticket.category` | `ticket.category_id`, nullable | `mappedBy = "category"` | Aucune |
| Ticket *—* Tag | `Ticket.tags` | Table `ticket_tag`, clé primaire composite | `mappedBy = "tags"` | Aucune |

Contraintes d'unicité : `technician.email`, `category.name`, `tag.name`, `technician_profile.technician_id`.

## Choix techniques

- **Le profil possède la clé étrangère.** La base garantit qu'un profil n'existe pas sans technicien. Contrepartie : Hibernate charge le profil dès qu'il charge un technicien ; utiliser un `@EntityGraph` ou un `join fetch` pour les listes.
- **Pas de remplacement direct du profil.** Hibernate insère avant de supprimer, ce qui violerait l'unicité. `assignProfile` refuse un second profil : le modifier, ou le retirer, vider le contexte (`flush`), puis en assigner un autre.
- **Suppressions.** La base refuse de supprimer un technicien, une catégorie ou un tag encore lié à un ticket. La couche Service vérifie d'abord et refuse proprement avec un message explicite. Supprimer un ticket retire ses lignes `ticket_tag` et conserve les tags.
- **Synchronisation.** Les méthodes `assign*`, `add*` et `remove*` mettent à jour les deux côtés. Les collections exposées sont en lecture seule.
- **Identité.** `equals` repose sur l'identifiant et `hashCode` est constant par type : stable avant et après persistance, compatible avec les proxys, sans récursion.
- **Dates.** `createdAt` et `updatedAt` sont gérées par `@PrePersist` et `@PreUpdate`. `resolvedAt` est posée par `TicketService` (horloge injectable).
- **Contraintes CHECK.** Hibernate génère les contraintes CHECK des enums (colonnes `VARCHAR`) avec le dialecte H2 standard. H2 2.4.240 les liait à la connexion créatrice de la table (erreur 23514 après sa fermeture, [issue 4302](https://github.com/h2database/h2database/issues/4302)) ; corrigé dans H2 2.5.250, version fixée à 2.5.252 dans le `pom.xml`. Les tests `insertsStillWorkOnceTheConnectionThatCreatedTheSchemaIsClosed` (non-régression) et `databaseRejectsInvalidEnumValues` (valeur invalide refusée) couvrent ce point.
- **Session fermée avant la vue** (`open-in-view=false`) : les données nécessaires doivent être chargées dans la couche Service.

## Persistance

| Contexte | URL | Schéma |
|---|---|---|
| Application | `jdbc:h2:file:./data/runtime/helpdesk` | `update` |
| Tests | Base H2 en mémoire ou fichier temporaire | `create-drop` (mémoire), `update` (fichier) |

Le chemin est relatif : lancer l'application depuis la racine du projet. L'initialisation est décrite dans [data/README.md](../data/README.md).

## Design system (mission 04.1)

Bootstrap 5.3.3 reste la fondation technique ; toute l'apparence est redéfinie dans `static/css/helpdesk.css` (aucun CDN, fonctionnement hors ligne).

- **Palette de marque.** Midnight `#0B1020`, Slate `#131C31`, Electric Violet `#7C5CFC`, Cyan `#22D3EE`, Cloud `#F8FAFC`. Le violet sert aux accents, aux icônes et aux fonds de boutons assombris (`#6C4CF4`) ; un violet éclairci (sombre) ou foncé (clair) sert au texte, pour garder un contraste AA.
- **Jetons.** Variables CSS `--hp-*` : surfaces, bordures, texte, accent, ombres, espacements (`--hp-s1` à `--hp-s7`), rayons, durées. Couleurs sémantiques : succès, avertissement, danger, une couleur par statut (`--st-*`) et par priorité (`--pr-*`), chacune avec sa variante pour le texte (`-fg`).
- **Thèmes.** Sombre par défaut, clair en option : l'attribut `data-bs-theme` de `<html>` pilote à la fois Bootstrap et les jetons. `theme.js`, chargé dans `<head>` avant les feuilles de style, applique le choix enregistré (`localStorage`, clé `hp-theme`) avant le premier affichage, sans clignotement. Si le stockage est bloqué, le choix dure le temps de la page.
- **Menu.** Barre latérale propre (pas de `offcanvas-lg` : voir ci-dessous), tiroir sur petit écran avec fond, touche Échap et retour du focus. Lien d'évitement « Aller au contenu ».
- **Palette de commandes.** `<dialog>` natif, ouvert par Ctrl+K (Cmd+K) ou par le bouton de la barre supérieure. Chaque entrée est une vraie route ; « Rechercher … dans les tickets » ouvre `/tickets?keyword=…` et un nombre ouvre `/tickets/{n}`. Navigation aux flèches, Entrée, Échap.
- **Notifications.** Les messages flash sont rendus dans des éléments masqués `.hp-flash` puis transformés en toasts par `helpdesk.js` dans une région `aria-live` : `role="status"` et disparition après quelques secondes pour un succès, `role="alert"` et fermeture manuelle pour une erreur. Sans JavaScript, un bloc `<noscript>` affiche les messages.
- **Tableau de bord.** Grille Bento CSS. Anneau par statut en SVG (segments calculés par `DashboardResponse.statusSegments()`), barres par priorité, anneau de respect des échéances (tickets résolus ou clôturés dans les délais, sinon état vide), listes d'interventions urgentes et d'incidents récents. Aucun chiffre n'est fixe.
- **Fiche ticket.** Indicateur de cycle de vie en quatre étapes ; seules les dates réelles (création, résolution) y figurent.
- **Kanban** (`/tickets/board`). Une colonne par statut, filtres de recherche, priorité, technicien et catégorie. Chaque carte n'offre que l'étape suivante autorisée (`TicketStatus.next()`), exécutée par `TicketService.changeStatus` ; la résolution, qui exige une solution, renvoie à la fiche. Après une action, retour au Kanban (paramètre `from=board`). Pas de glisser-déposer : les actions passent par des boutons, plus fiables et accessibles au clavier.
- **Accessibilité.** Focus visible, `aria-current` sur la page active, libellés des boutons-icônes, graphiques SVG étiquetés, contrastes AA mesurés dans les deux thèmes sur quatorze pages, `prefers-reduced-motion` respecté (animations et transitions neutralisées).
- **Cause du défaut visuel initial.** La classe Bootstrap `.offcanvas-lg` impose `background-color: transparent !important` et `width: auto !important` dès 992 px : le fond bleu nuit du menu disparaissait et son texte clair devenait illisible sur le fond clair. De plus `.card` est une colonne flex, ce qui empilait icône et valeur des tuiles. Le menu n'utilise plus `offcanvas-lg` et ses styles sont les siens.
- **Cache.** `spring.web.resources.chain.strategy.content` ajoute l'empreinte du contenu aux URL des ressources statiques (`helpdesk-<hash>.css`) : un fichier modifié n'est jamais servi depuis un cache périmé.
