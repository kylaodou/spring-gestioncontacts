// Modification de Groupe.java
package com.anderson.gestioncontacts.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
public class Groupe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @Column(length = 1000)
    private String description;

    @ManyToMany
    @JoinTable(
            name = "groupe_contact",
            joinColumns = @JoinColumn(name = "groupe_id"),
            inverseJoinColumns = @JoinColumn(name = "contact_id")
    )
    @ToString.Exclude // Prevent stack overflow in toString()
    @EqualsAndHashCode.Exclude // Prevent stack overflow in equals/hashCode
    private Set<Contact> contacts = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}