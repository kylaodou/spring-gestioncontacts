package com.anderson.gestioncontacts.service;

import com.anderson.gestioncontacts.model.Contact;
import com.anderson.gestioncontacts.model.User;
import com.anderson.gestioncontacts.repository.ContactRepository;
import com.anderson.gestioncontacts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final String UPLOAD_DIR = "uploads/profil/";

    @Autowired
    public ContactService(ContactRepository contactRepository, UserRepository userRepository) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }

    public List<Contact> getContactsByUserId(Long userId) {
        return contactRepository.findByUserId(userId);
    }

    // Méthode pour vérifier si l'email existe déjà
    public boolean emailExists(String email) {
        return contactRepository.existsByEmail(email);
    }

    // Méthode pour vérifier si le téléphone existe déjà
    public boolean telephoneExists(String telephone) {
        return contactRepository.existsByTelephone(telephone);
    }

    // Méthodes pour vérifier si l'email existe déjà pour un autre utilisateur
    public boolean emailExistsForOtherUser(String email, Long userId) {
        return contactRepository.existsByEmailAndUserIdNot(email, userId);
    }

    // Méthode pour vérifier si le téléphone existe déjà pour un autre utilisateur
    public boolean telephoneExistsForOtherUser(String telephone, Long userId) {
        return contactRepository.existsByTelephoneAndUserIdNot(telephone, userId);
    }

    public Optional<Contact> getContactById(Long id) {
        return contactRepository.findById(id);
    }

    public Contact createContact(Contact contact, MultipartFile photoFile, Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id: " + userId));

        contact.setUser(user);

        if (photoFile != null && !photoFile.isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + photoFile.getOriginalFilename();
            Path uploadPath = Paths.get(UPLOAD_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Files.copy(photoFile.getInputStream(), uploadPath.resolve(fileName));
            contact.setPhotoProfil(fileName);
        }

        return contactRepository.save(contact);
    }

    public Contact updateContact(Long id, Contact contactDetails, MultipartFile photoFile) throws IOException {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact non trouvé avec l'id: " + id));

        contact.setNom(contactDetails.getNom());
        contact.setPrenom(contactDetails.getPrenom());
        contact.setEmail(contactDetails.getEmail());
        contact.setTelephone(contactDetails.getTelephone());
        contact.setNote(contactDetails.getNote());

        if (photoFile != null && !photoFile.isEmpty()) {
            // Supprimer l'ancienne photo si elle existe
            if (contact.getPhotoProfil() != null) {
                Path oldPhotoPath = Paths.get(UPLOAD_DIR + contact.getPhotoProfil());
                Files.deleteIfExists(oldPhotoPath);
            }

            String fileName = UUID.randomUUID().toString() + "_" + photoFile.getOriginalFilename();
            Path uploadPath = Paths.get(UPLOAD_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Files.copy(photoFile.getInputStream(), uploadPath.resolve(fileName));
            contact.setPhotoProfil(fileName);
        }

        return contactRepository.save(contact);
    }

    public void deleteContact(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact non trouvé avec l'id: " + id));

        // Supprimer la photo de profil si elle existe
        if (contact.getPhotoProfil() != null) {
            try {
                Path photoPath = Paths.get(UPLOAD_DIR + contact.getPhotoProfil());
                Files.deleteIfExists(photoPath);
            } catch (IOException e) {
                // Gérer l'exception
            }
        }

        contactRepository.deleteById(id);
    }
}