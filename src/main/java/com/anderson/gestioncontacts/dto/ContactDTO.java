// Modification de ContactDTO.java
package com.anderson.gestioncontacts.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ContactDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String note;
    private MultipartFile photo;
    private String photoProfilUrl;
    private Long userId;  // Ajout de l'ID de l'utilisateur
}