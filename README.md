# FoodNDeliv : plateforme applicative fiable sur Kubernetes

Projet réalisé à l'**Université de Malte** (module CPS2010, *Reliable Application Platforms*, 2026) :
déploiement d'une API de commande de repas sur Kubernetes, pensée pour la haute disponibilité, la sécurité
et la résistance aux pics de charge.

## Architecture

- **Deux services Spring Boot** : `foodndeliv-rw` (lecture-écriture) et `foodndeliv-ro` (lecture seule).
  Les lectures, majoritaires (consultation des menus), sont envoyées vers des répliques que l'on peut multiplier
  sans charger la base principale.
- **PostgreSQL en haute disponibilité** avec l'opérateur **CloudNativePG** (installé par Helm) : une instance
  primaire, des répliques en réplication continue et une bascule automatique en cas de panne.
- **Passerelle d'API KrakenD** devant les services, avec validation des jetons **JWT**.
- **Keycloak** pour l'authentification (OIDC) : connexion, déconnexion et rôles (administrateur, client).
- **Sondes Kubernetes** (liveness, readiness) pour redémarrer ou isoler un pod défaillant.
- Instrumentation **OpenTelemetry** et **Jaeger** pour le traçage.

## Points notables

- Principe du moindre privilège : le service en lecture seule utilise des identifiants qui ne permettent pas
  d'écrire en base.
- Vérification côté service qu'un client ne modifie que son propre compte (claim `preferred_username` du JWT).
- Endpoint d'administration `bulk-reset-passwords` protégé par rôle (`@PreAuthorize`) et par la passerelle,
  avec journalisation d'audit.

## Technologies

Java 17 · Spring Boot · Spring Security · PostgreSQL · CloudNativePG · Kubernetes (Minikube) · Helm · Docker ·
KrakenD · Keycloak · OpenTelemetry · Jaeger

## Contenu du dépôt

| Dossier / fichier | Contenu |
|---|---|
| `foodndeliv-rw/`, `foodndeliv-ro/` | les deux services Spring Boot |
| `k8s/` | manifestes Kubernetes (PostgreSQL, Keycloak, applications) |
| `krakend/` | configuration de la passerelle d'API |
| `INSTRUCTIONS.md` | déploiement pas à pas sur Minikube |
| `report.md` | rapport du projet (en anglais) |

## Lancer le projet

Voir [`INSTRUCTIONS.md`](INSTRUCTIONS.md). Les mots de passe des fichiers de configuration valent `change-me`
et sont à remplacer avant tout déploiement. L'agent OpenTelemetry (`opentelemetry-javaagent.jar`) n'est pas
versionné : il se télécharge depuis
[le dépôt officiel](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases).
