package com.anderson.gestioncontacts.service;

import com.anderson.gestioncontacts.model.Contact;
import com.anderson.gestioncontacts.model.Groupe;
import com.anderson.gestioncontacts.model.User;
import com.anderson.gestioncontacts.repository.ContactRepository;
import com.anderson.gestioncontacts.repository.GroupeRepository;
import com.anderson.gestioncontacts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class GroupeService {

    private final GroupeRepository groupeRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    @Autowired
    public GroupeService(GroupeRepository groupeRepository, ContactRepository contactRepository, UserRepository userRepository) {
        this.groupeRepository = groupeRepository;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    public List<Groupe> getAllGroupes() {
        return groupeRepository.findAll();
    }

    public List<Groupe> getGroupesByUserId(Long userId) {
        return groupeRepository.findByUserId(userId);
    }

    public Optional<Groupe> getGroupeById(Long id) {
        return groupeRepository.findById(id);
    }
    public List<Groupe> getGroupesByContactId(Long contactId) {
        // Implémentation pour récupérer les groupes d'un contact
        return groupeRepository.findByContactsId(contactId);
    }
    // Add this method to check if a group name already exists for a specific user
    // Dans GroupeService
    public boolean existsByNomAndUserId(String nom, Long userId) {
        boolean exists = groupeRepository.existsByNomAndUserId(nom, userId);
        System.out.println("Vérification de l'existence - Nom: " + nom + ", UserId: " + userId + ", Résultat: " + exists);
        return exists;
    }

    // Add this method to check if a group name exists excluding the current group for a specific user
    public boolean existsByNomAndIdNotAndUserId(String nom, Long id, Long userId) {
        return groupeRepository.existsByNomAndIdNotAndUserId(nom, id, userId);
    }

    @Transactional
    public Groupe createGroupe(Groupe groupe, Set<Long> contactIds, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id: " + userId));

        // Check if a group with the same name already exists for this user
        if (existsByNomAndUserId(groupe.getNom(), userId)) {
            throw new IllegalArgumentException("Un groupe avec ce nom existe déjà");
        }

        groupe.setUser(user);
        Groupe savedGroupe = groupeRepository.save(groupe);

        if (contactIds != null && !contactIds.isEmpty()) {
            updateGroupeContacts(savedGroupe, contactIds, userId);
        }

        return savedGroupe;
    }

    @Transactional
    public Groupe updateGroupe(Long id, Groupe groupeDetails, Set<Long> contactIds, Long userId) {
        Groupe groupe = groupeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé avec l'id: " + id));

        // Verify that the group belongs to the user
        if (!groupe.getUser().getId().equals(userId)) {
            throw new RuntimeException("Ce groupe n'appartient pas à l'utilisateur");
        }

        // Check if another group with the same name already exists for this user
        if (existsByNomAndIdNotAndUserId(groupeDetails.getNom(), id, userId)) {
            throw new IllegalArgumentException("Un groupe avec ce nom existe déjà");
        }

        groupe.setNom(groupeDetails.getNom());
        groupe.setDescription(groupeDetails.getDescription());

        if (contactIds != null) {
            updateGroupeContacts(groupe, contactIds, userId);
        }

        return groupeRepository.save(groupe);
    }

    private void updateGroupeContacts(Groupe groupe, Set<Long> contactIds, Long userId) {
        // Clear existing contacts
        groupe.getContacts().clear();

        // Add new contacts
        if (!contactIds.isEmpty()) {
            List<Contact> contacts = contactRepository.findAllById(contactIds);
            for (Contact contact : contacts) {
                // Verify that each contact belongs to the same user
                if (contact.getUser().getId().equals(userId)) {
                    groupe.getContacts().add(contact);
                }
            }
        }
    }

    public void deleteGroupe(Long id, Long userId) {
        Groupe groupe = groupeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé avec l'id: " + id));

        // Verify that the group belongs to the user
        if (!groupe.getUser().getId().equals(userId)) {
            throw new RuntimeException("Ce groupe n'appartient pas à l'utilisateur");
        }

        groupeRepository.deleteById(id);
    }

    @Transactional
    public Groupe addContactToGroupe(Long groupeId, Long contactId, Long userId) {
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé avec l'id: " + groupeId));

        // Verify that the group belongs to the user
        if (!groupe.getUser().getId().equals(userId)) {
            throw new RuntimeException("Ce groupe n'appartient pas à l'utilisateur");
        }

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact non trouvé avec l'id: " + contactId));

        // Verify that the contact belongs to the user
        if (!contact.getUser().getId().equals(userId)) {
            throw new RuntimeException("Ce contact n'appartient pas à l'utilisateur");
        }

        groupe.getContacts().add(contact);
        return groupeRepository.save(groupe);
    }
    public boolean existsByNomAndIdNot(String nom, Long id) {
        return groupeRepository.existsByNomAndIdNot(nom, id);
    }
    public boolean existsByNom(String nom) {
        return groupeRepository.existsByNom(nom);
    }
    @Transactional
    public Groupe removeContactFromGroupe(Long groupeId, Long contactId, Long userId) {
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé avec l'id: " + groupeId));

        // Verify that the group belongs to the user
        if (!groupe.getUser().getId().equals(userId)) {
            throw new RuntimeException("Ce groupe n'appartient pas à l'utilisateur");
        }

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact non trouvé avec l'id: " + contactId));

        groupe.getContacts().remove(contact);
        return groupeRepository.save(groupe);
    }
}