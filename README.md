# Gestion de contacts — API Spring Boot

API REST de gestion de carnet d'adresses : chaque utilisateur gère ses contacts (avec photo) et
les range dans des groupes. L'accès est protégé par des jetons JWT.

## Fonctionnalités

- **Authentification** : inscription, connexion (jeton JWT), utilisateur courant, changement de
  mot de passe, déconnexion.
- **Utilisateurs** : liste, détail, modification.
- **Contacts** : création et modification avec envoi d'une photo (multipart), liste, liste par
  utilisateur, détail, suppression, liste avec images et groupes.
- **Groupes** : création, modification, suppression, ajout et retrait d'un contact.
- **Images** : servies publiquement par `/api/contacts/api/public/images/{fichier}`.
- Filtre d'authentification JWT, configuration CORS.

## Routes

| Méthode | Route |
|---|---|
| POST | `/api/auth/register`, `/api/auth/login` |
| GET | `/me`, `/users`, `/users/{id}` |
| PUT | `/users/{id}` |
| POST | `/change-password`, `/logout` |
| GET, POST | `/api/contacts` |
| GET, PUT, DELETE | `/api/contacts/{id}` |
| GET | `/api/contacts/user/{userId}`, `/api/contacts/with-images-and-groupes` |
| GET, POST | `/api/groupes` |
| GET, PUT, DELETE | `/api/groupes/{id}` |
| POST, DELETE | `/api/groupes/{groupeId}/contacts/{contactId}` |

Les routes protégées attendent l'en-tête `Authorization: Bearer <jeton>`.

## Stack technique

- Java 17, Spring Boot 3.4 (Web, Data JPA, Security)
- PostgreSQL
- JJWT (jetons JWT)
- Lombok
- Maven

## Prérequis

- JDK 17
- PostgreSQL 14+

## Configuration

Créez la base :

```sql
CREATE DATABASE gestioncontact;
```

Puis définissez les variables d'environnement (aucun mot de passe n'est écrit dans le dépôt) :

```bash
export DB_URL=jdbc:postgresql://localhost:5432/gestioncontact
export DB_USERNAME=postgres
export DB_PASSWORD=votre_mot_de_passe
export JWT_SECRET=une_longue_cle_aleatoire_de_32_caracteres_minimum
```

Sans `JWT_SECRET`, l'application refuse de démarrer.

Les tables sont créées automatiquement (`ddl-auto=update`). Les photos sont enregistrées dans le
dossier `./uploads/` (paramètre `file.upload-dir`).

## Lancement

```bash
git clone https://github.com/kylaodou/spring-gestioncontacts.git
cd spring-gestioncontacts
./mvnw spring-boot:run
```

L'API écoute sur http://localhost:8080.

## Auteur

[@kylaodou](https://github.com/kylaodou)
