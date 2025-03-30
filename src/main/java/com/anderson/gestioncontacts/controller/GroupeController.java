package com.anderson.gestioncontacts.controller;

import com.anderson.gestioncontacts.dto.GroupeDTO;
import com.anderson.gestioncontacts.model.Groupe;
import com.anderson.gestioncontacts.model.User;
import com.anderson.gestioncontacts.repository.UserRepository;
import com.anderson.gestioncontacts.service.GroupeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groupes")
public class GroupeController {

    private final GroupeService groupeService;

    private final UserRepository userRepository;

    @Autowired
    public GroupeController(GroupeService groupeService, UserRepository userRepository) {
        this.groupeService = groupeService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Récupérer l'utilisateur à partir de l'email
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email: " + userEmail));

        return user.getId();
    }

    @GetMapping
    public ResponseEntity<List<Groupe>> getAllGroupes() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(groupeService.getGroupesByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Groupe> getGroupeById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return groupeService.getGroupeById(id)
                .filter(groupe -> groupe.getUser().getId().equals(userId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createGroupe(@RequestBody GroupeDTO groupeDTO) {
        try {
            Long userId = getCurrentUserId();

            // Vérifier si un groupe avec le même nom existe déjà pour cet utilisateur
            if (groupeService.existsByNomAndUserId(groupeDTO.getNom(), userId)) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "nom_exists");
                response.put("message", "Un groupe avec ce nom existe déjà.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            Groupe groupe = new Groupe();
            groupe.setNom(groupeDTO.getNom());
            groupe.setDescription(groupeDTO.getDescription());

            Groupe savedGroupe = groupeService.createGroupe(groupe, groupeDTO.getContactIds(), userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedGroupe);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "validation_error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "not_found");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "server_error");
            response.put("message", "Une erreur s'est produite lors de la création du groupe");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGroupe(@PathVariable Long id, @RequestBody GroupeDTO groupeDTO) {
        try {
            Long userId = getCurrentUserId();

            // Récupérer le groupe existant
            Groupe existingGroupe = groupeService.getGroupeById(id)
                    .orElseThrow(() -> new RuntimeException("Groupe non trouvé avec l'id: " + id));

            // Vérifier que le groupe appartient à l'utilisateur
            if (!existingGroupe.getUser().getId().equals(userId)) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "unauthorized");
                response.put("message", "Ce groupe n'appartient pas à l'utilisateur.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Vérifier si un autre groupe avec le même nom existe déjà pour cet utilisateur
            if (!groupeDTO.getNom().equals(existingGroupe.getNom()) &&
                    groupeService.existsByNomAndIdNotAndUserId(groupeDTO.getNom(), id, userId)) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "nom_exists");
                response.put("message", "Un groupe avec ce nom existe déjà.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            Groupe groupe = new Groupe();
            groupe.setNom(groupeDTO.getNom());
            groupe.setDescription(groupeDTO.getDescription());

            Groupe updatedGroupe = groupeService.updateGroupe(id, groupe, groupeDTO.getContactIds(), userId);
            return ResponseEntity.ok(updatedGroupe);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "validation_error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "not_found");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "server_error");
            response.put("message", "Une erreur s'est produite lors de la mise à jour du groupe");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGroupe(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            groupeService.deleteGroupe(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/{groupeId}/contacts/{contactId}")
    public ResponseEntity<?> addContactToGroupe(@PathVariable Long groupeId, @PathVariable Long contactId) {
        try {
            Long userId = getCurrentUserId();
            Groupe updatedGroupe = groupeService.addContactToGroupe(groupeId, contactId, userId);
            return ResponseEntity.ok(updatedGroupe);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @DeleteMapping("/{groupeId}/contacts/{contactId}")
    public ResponseEntity<?> removeContactFromGroupe(@PathVariable Long groupeId, @PathVariable Long contactId) {
        try {
            Long userId = getCurrentUserId();
            Groupe updatedGroupe = groupeService.removeContactFromGroupe(groupeId, contactId, userId);
            return ResponseEntity.ok(updatedGroupe);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}