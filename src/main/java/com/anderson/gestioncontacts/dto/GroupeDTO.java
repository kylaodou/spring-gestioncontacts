// Modification de GroupeDTO.java
package com.anderson.gestioncontacts.dto;

import lombok.Data;
import java.util.Set;

@Data
public class GroupeDTO {
    private Long id;
    private String nom;
    private String description;
    private Set<Long> contactIds;
    private Long userId;  // Ajout de l'ID de l'utilisateur
}