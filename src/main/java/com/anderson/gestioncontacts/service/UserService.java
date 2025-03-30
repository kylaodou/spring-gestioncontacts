package com.anderson.gestioncontacts.service;

import com.anderson.gestioncontacts.model.User;
import com.anderson.gestioncontacts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Méthode pour enregistrer un utilisateur
    public User register(User user) {
        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("L'email est déjà utilisé");
        }

        // Encoder le mot de passe
        user.setMotDePasse(passwordEncoder.encode(user.getMotDePasse()));

        return userRepository.save(user);
    }

    // Méthode pour la connexion d'un utilisateur
    public Optional<User> login(String email, String motDePasse) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(motDePasse, user.getMotDePasse())) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);  // Assurez-vous d'avoir cette méthode dans votre repository
    }

    // Ajout de la méthode findByEmail qui est utilisée dans AuthenticationFilter
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Récupérer tous les utilisateurs
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Récupérer un utilisateur par ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Mettre à jour un utilisateur
    public User updateUser(Long id, User userDetails) {
        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        User existingUser = optionalUser.get();

        // Mettre à jour les champs
        if (userDetails.getNom() != null) {
            existingUser.setNom(userDetails.getNom());
        }

        if (userDetails.getPrenom() != null) {
            existingUser.setPrenom(userDetails.getPrenom());
        }

        // Si email est modifié, vérifier qu'il n'existe pas déjà
        if (userDetails.getEmail() != null && !userDetails.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.findByEmail(userDetails.getEmail()).isPresent()) {
                throw new RuntimeException("L'email est déjà utilisé");
            }
            existingUser.setEmail(userDetails.getEmail());
        }

        // Si mot de passe fourni, l'encoder
        if (userDetails.getMotDePasse() != null && !userDetails.getMotDePasse().isEmpty()) {
            existingUser.setMotDePasse(passwordEncoder.encode(userDetails.getMotDePasse()));
        }

        if (userDetails.getPhotoProfil() != null) {
            existingUser.setPhotoProfil(userDetails.getPhotoProfil());
        }

        return userRepository.save(existingUser);
    }

    // Supprimer un utilisateur
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        userRepository.deleteById(id);
    }
}