// Modification de Contact.java
package com.anderson.gestioncontacts.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    @Column(length = 1000)
    private String note;

    private String photoProfil;

    @ManyToMany(mappedBy = "contacts")
    @JsonIgnore // Prevent infinite recursion during serialization
    @ToString.Exclude // Prevent stack overflow in toString()
    @EqualsAndHashCode.Exclude // Prevent stack overflow in equals/hashCode
    private Set<Groupe> groupes = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}