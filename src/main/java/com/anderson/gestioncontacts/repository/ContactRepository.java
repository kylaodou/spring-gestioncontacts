// Modification de ContactRepository.java
package com.anderson.gestioncontacts.repository;

import com.anderson.gestioncontacts.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    boolean existsByEmail(String email);
    boolean existsByTelephone(String telephone);
    List<Contact> findByUserId(Long userId);
    boolean existsByEmailAndUserIdNot(String email, Long userId);
    boolean existsByTelephoneAndUserIdNot(String telephone, Long userId);
}
