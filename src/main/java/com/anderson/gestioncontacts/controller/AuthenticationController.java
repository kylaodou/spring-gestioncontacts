package com.anderson.gestioncontacts.controller;

import com.anderson.gestioncontacts.dto.LoginDTO;
import com.anderson.gestioncontacts.dto.UserDTO;
import com.anderson.gestioncontacts.model.User;
import com.anderson.gestioncontacts.config.JwtUtil;
import com.anderson.gestioncontacts.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.security.Principal;

@RestController
@CrossOrigin(origins = "http://localhost:4200")

public class AuthenticationController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthenticationController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }


    @PostMapping("/api/auth/register")
    public ResponseEntity<?> register(@RequestBody(required = false) UserDTO userDTO) {
        // Vérifier si le corps de la requête est vide
        if (userDTO == null) {
            Map<String, String> errors = new HashMap<>();
            errors.put("message", "Le corps de la requête est vide");
            errors.put("nom", "Le nom est obligatoire");
            errors.put("prenom", "Le prénom est obligatoire");
            errors.put("email", "L'email est obligatoire");
            errors.put("motDePasse", "Le mot de passe est obligatoire");
            return ResponseEntity.badRequest().body(errors);
        }

        // Vérification des champs obligatoires
        Map<String, String> errors = new HashMap<>();

        if (userDTO.getNom() == null || userDTO.getNom().isEmpty()) {
            errors.put("nom", "Le nom est obligatoire");
        }
        if (userDTO.getPrenom() == null || userDTO.getPrenom().isEmpty()) {
            errors.put("prenom", "Le prénom est obligatoire");
        }
        if (userDTO.getEmail() == null || userDTO.getEmail().isEmpty()) {
            errors.put("email", "L'email est obligatoire");
        }
        if (userDTO.getMotDePasse() == null || userDTO.getMotDePasse().isEmpty()) {
            errors.put("motDePasse", "Le mot de passe est obligatoire");
        }

        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            // Vérification si l'email existe déjà dans la base de données
            if (userService.existsByEmail(userDTO.getEmail())) {
                Map<String, String> emailError = new HashMap<>();
                emailError.put("email", "L'email existe déjà");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(emailError);
            }

            // Création de l'utilisateur
            User user = new User();
            user.setNom(userDTO.getNom());
            user.setPrenom(userDTO.getPrenom());
            user.setEmail(userDTO.getEmail());
            user.setMotDePasse(userDTO.getMotDePasse());
            user.setPhotoProfil(userDTO.getPhotoProfil());

            User registeredUser = userService.register(user);

            // Ne pas renvoyer le mot de passe
            registeredUser.setMotDePasse(null);

            // Message personnalisé pour l'utilisateur créé
            Map<String, Object> successMessage = new HashMap<>();
            successMessage.put("message", "Utilisateur créé avec succès");
            successMessage.put("user", user);

            // Inclure la photo de profil si elle est disponible
            if (registeredUser.getPhotoProfil() != null) {
                successMessage.put("photoProfil", registeredUser.getPhotoProfil());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(successMessage);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        // Vérifier si le corps de la requête est vide
        if (loginDTO == null) {
            Map<String, String> errors = new HashMap<>();
            errors.put("message", "Le corps de la requête est vide");
            errors.put("email", "L'email est obligatoire");
            errors.put("motDePasse", "Le mot de passe est obligatoire");
            return ResponseEntity.badRequest().body(errors);
        }

        // Vérification des champs obligatoires
        Map<String, String> errors = new HashMap<>();

        if (loginDTO.getEmail() == null || loginDTO.getEmail().isEmpty()) {
            errors.put("email", "L'email est obligatoire");
        }
        if (loginDTO.getMotDePasse() == null || loginDTO.getMotDePasse().isEmpty()) {
            errors.put("motDePasse", "Le mot de passe est obligatoire");
        }

        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(errors);
        }

        // Recherche de l'utilisateur dans la base de données
        Optional<User> optionalUser = userService.login(loginDTO.getEmail(), loginDTO.getMotDePasse());

        // Vérification si l'utilisateur existe et si le mot de passe est correct
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            // Générer un token JWT
            String token = jwtUtil.generateToken(user.getEmail());

            // Ne pas renvoyer le mot de passe
            User userResponse = new User();
            userResponse.setId(user.getId());
            userResponse.setNom(user.getNom());
            userResponse.setPrenom(user.getPrenom());
            userResponse.setEmail(user.getEmail());
            userResponse.setPhotoProfil(user.getPhotoProfil());

            // Préparer la réponse avec le token et les informations de l'utilisateur
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Connexion réussie");
            response.put("token", token);
            response.put("user", userResponse);

            return ResponseEntity.ok(response);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Email ou mot de passe incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }


    // Récupérer tous les utilisateurs
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        // Masquer les mots de passe avant de renvoyer la liste
        users.forEach(user -> user.setMotDePasse(null));
        return ResponseEntity.ok(users);
    }

    // Récupérer un utilisateur par ID
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> userOptional = userService.getUserById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setMotDePasse(null); // Masquer le mot de passe
            return ResponseEntity.ok(user);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Utilisateur non trouvé");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // Récupérer l'utilisateur actuellement connecté
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal != null) {
            Optional<User> userOptional = userService.findByEmail(principal.getName());
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setMotDePasse(null); // Masquer le mot de passe
                return ResponseEntity.ok(user);
            }
        }
        Map<String, String> response = new HashMap<>();
        response.put("error", "Utilisateur non authentifié ou introuvable");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    // Méthode pour mettre à jour le profil utilisateur
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO, Principal principal) {
        // Vérifier si l'utilisateur est authentifié
        if (principal == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Utilisateur non authentifié");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Vérifier si l'utilisateur tente de modifier son propre profil
        Optional<User> currentUserOpt = userService.findByEmail(principal.getName());
        if (currentUserOpt.isEmpty() || !currentUserOpt.get().getId().equals(id)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Vous n'êtes pas autorisé à modifier ce profil");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            // Vérification des champs obligatoires
            Map<String, String> errors = new HashMap<>();
            if (userDTO.getNom() == null || userDTO.getNom().trim().isEmpty()) {
                errors.put("nom", "Le nom est obligatoire");
            }
            if (userDTO.getPrenom() == null || userDTO.getPrenom().trim().isEmpty()) {
                errors.put("prenom", "Le prénom est obligatoire");
            }
            if (userDTO.getEmail() == null || userDTO.getEmail().trim().isEmpty()) {
                errors.put("email", "L'email est obligatoire");
            }

            // Si des erreurs sont détectées, retourner une réponse BAD_REQUEST avec les détails
            if (!errors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Données incomplètes ou incorrectes");
                response.put("errors", errors);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Création d'un objet User avec les données mises à jour
            User userToUpdate = new User();
            userToUpdate.setNom(userDTO.getNom());
            userToUpdate.setPrenom(userDTO.getPrenom());
            userToUpdate.setEmail(userDTO.getEmail());
            userToUpdate.setPhotoProfil(userDTO.getPhotoProfil());

            // Ne pas mettre à jour le mot de passe ici, c'est une opération distincte

            User updatedUser = userService.updateUser(id, userToUpdate);

            // Ne pas renvoyer le mot de passe
            updatedUser.setMotDePasse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profil mis à jour avec succès");
            response.put("user", updatedUser);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Erreur lors de la mise à jour du profil");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Méthode pour changer le mot de passe
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordData, Principal principal) {
        // Vérifier si l'utilisateur est authentifié
        if (principal == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Utilisateur non authentifié");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Vérifier si les données nécessaires sont présentes
        Map<String, String> errors = new HashMap<>();
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");

        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            errors.put("oldPassword", "L'ancien mot de passe est obligatoire");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            errors.put("newPassword", "Le nouveau mot de passe est obligatoire");
        } else if (newPassword.length() < 8) {
            errors.put("newPassword", "Le nouveau mot de passe doit contenir au moins 8 caractères");
        }

        // Si des erreurs sont détectées, retourner une réponse avec les détails
        if (!errors.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Données incomplètes ou incorrectes");
            response.put("errors", errors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            // Récupérer l'utilisateur connecté
            Optional<User> userOptional = userService.findByEmail(principal.getName());
            if (userOptional.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Utilisateur non trouvé");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            User user = userOptional.get();

            // Vérifier si l'ancien mot de passe est correct
            Optional<User> authenticatedUser = userService.login(user.getEmail(), oldPassword);
            if (authenticatedUser.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Authentification échouée");
                response.put("errors", Map.of("oldPassword", "Ancien mot de passe incorrect"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Mettre à jour le mot de passe
            User userToUpdate = new User();
            userToUpdate.setMotDePasse(newPassword);

            userService.updateUser(user.getId(), userToUpdate);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Mot de passe modifié avec succès");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Erreur lors du changement de mot de passe");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    // Méthode pour la déconnexion
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Spring Security gère déjà la déconnexion côté serveur avec l'invalidation de session
        // Mais dans une API REST avec JWT, la déconnexion est principalement gérée côté client
        // en supprimant le token

        Map<String, String> response = new HashMap<>();
        response.put("message", "Déconnexion réussie");
        return ResponseEntity.ok(response);

        // Note: Pour une mise en œuvre plus complète, vous pourriez ajouter:
        // 1. Un système de liste noire de tokens
        // 2. Des tokens avec une courte durée de vie + refresh token
    }
}