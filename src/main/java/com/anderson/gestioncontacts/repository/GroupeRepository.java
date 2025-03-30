// Modification de GroupeRepository.java
package com.anderson.gestioncontacts.repository;

import com.anderson.gestioncontacts.model.Groupe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupeRepository extends JpaRepository<Groupe, Long> {
    boolean existsByNom(String nom);
    boolean existsByNomAndIdNot(String nom, Long id);
    List<Groupe> findByUserId(Long userId);
    boolean existsByNomAndUserId(String nom, Long userId);
    boolean existsByNomAndIdNotAndUserId(String nom, Long id, Long userId);
    List<Groupe> findByContactsId(Long contactId);
}