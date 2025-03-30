package com.anderson.gestioncontacts.controller;

import com.anderson.gestioncontacts.model.Contact;
import com.anderson.gestioncontacts.model.Groupe;
import com.anderson.gestioncontacts.service.ContactService;
import com.anderson.gestioncontacts.service.GroupeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;
    private final GroupeService groupeService;

    @Autowired
    public ContactController(ContactService contactService, GroupeService groupeService) {
        this.contactService = contactService;
        this.groupeService = groupeService;
    }

    @GetMapping
    public ResponseEntity<List<Contact>> getAllContacts() {
        return ResponseEntity.ok(contactService.getAllContacts());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Contact>> getContactsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(contactService.getContactsByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contact> getContactById(@PathVariable Long id) {
        return contactService.getContactById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createContact(
            @RequestParam("nom") String nom,
            @RequestParam("prenom") String prenom,
            @RequestParam("email") String email,
            @RequestParam("telephone") String telephone,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam("userId") Long userId) {

        // Vérifier si l'email existe déjà
        if (contactService.emailExists(email)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "email_exists");
            response.put("message", "Cet email est déjà utilisé par un autre contact.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        // Vérifier si le téléphone existe déjà
        if (contactService.telephoneExists(telephone)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "telephone_exists");
            response.put("message", "Ce numéro de téléphone est déjà utilisé par un autre contact.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        try {
            Contact contact = new Contact();
            contact.setNom(nom);
            contact.setPrenom(prenom);
            contact.setEmail(email);
            contact.setTelephone(telephone);
            contact.setNote(note);

            Contact savedContact = contactService.createContact(contact, photo, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedContact);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/public/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            // Utilisez un chemin absolu ou relatif à votre application
            Path imagePath = Paths.get(System.getProperty("user.dir"), "uploads/profil", filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.IMAGE_JPEG) // ou déterminer dynamiquement
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace(); // Ajoutez ceci pour déboguer
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateContact(
            @PathVariable Long id,
            @RequestParam("nom") String nom,
            @RequestParam("prenom") String prenom,
            @RequestParam("email") String email,
            @RequestParam("telephone") String telephone,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam("userId") Long userId) {

        try {
            // Récupérer le contact existant
            Contact existingContact = contactService.getContactById(id)
                    .orElseThrow(() -> new RuntimeException("Contact non trouvé avec l'id: " + id));

            // Vérifier que le contact appartient à l'utilisateur
            if (!existingContact.getUser().getId().equals(userId)) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "unauthorized");
                response.put("message", "Ce contact n'appartient pas à l'utilisateur.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Vérifier si l'email existe déjà pour un autre contact
            if (!email.equals(existingContact.getEmail()) && contactService.emailExistsForOtherUser(email, userId)) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "email_exists");
                response.put("message", "Cet email est déjà utilisé par un autre contact.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // Vérifier si le téléphone existe déjà pour un autre contact
            if (!telephone.equals(existingContact.getTelephone()) && contactService.telephoneExistsForOtherUser(telephone, userId)) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "telephone_exists");
                response.put("message", "Ce numéro de téléphone est déjà utilisé par un autre contact.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            Contact contact = new Contact();
            contact.setNom(nom);
            contact.setPrenom(prenom);
            contact.setEmail(email);
            contact.setTelephone(telephone);
            contact.setNote(note);

            Contact updatedContact = contactService.updateContact(id, contact, photo);
            return ResponseEntity.ok(updatedContact);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "not_found");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "server_error");
            response.put("message", "Erreur lors du traitement de l'image");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/with-images-and-groupes")
    public ResponseEntity<List<Map<String, Object>>> getContactsWithImagesAndGroupes(@RequestParam Long userId) {
        List<Contact> contacts = contactService.getContactsByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Contact contact : contacts) {
            Map<String, Object> contactMap = new HashMap<>();
            contactMap.put("contact", contact);

            // Ajouter le chemin de l'image si disponible
            if (contact.getPhotoProfil() != null && !contact.getPhotoProfil().isEmpty()) {
                String imagePath = contact.getPhotoProfil();
                contactMap.put("imagePath", imagePath);
            }

            // Récupérer les groupes du contact
            List<Groupe> groupes = groupeService.getGroupesByContactId(contact.getId());
            contactMap.put("groupes", groupes);

            result.add(contactMap);
        }

        return ResponseEntity.ok(result);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContact(@PathVariable Long id, @RequestParam("userId") Long userId) {
        try {
            // Récupérer le contact existant
            Contact existingContact = contactService.getContactById(id)
                    .orElseThrow(() -> new RuntimeException("Contact non trouvé avec l'id: " + id));

            // Vérifier que le contact appartient à l'utilisateur
            if (!existingContact.getUser().getId().equals(userId)) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "unauthorized");
                response.put("message", "Ce contact n'appartient pas à l'utilisateur.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            contactService.deleteContact(id);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Contact supprimé avec succès");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "not_found");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}