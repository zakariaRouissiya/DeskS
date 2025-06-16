package com.symolia.DeskS.repositories;

import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    List<Utilisateur> findByRole(Role role);
    List<Utilisateur> findByDepartment_Id(Long departmentId);
    List<Utilisateur> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM Utilisateur u WHERE (:role IS NULL OR u.role = :role) " +
            "AND (:departmentId IS NULL OR u.department.id = :departmentId) " +
            "AND (:nom IS NULL OR u.nom LIKE :nom || '%') " +
            "AND (:prenom IS NULL OR u.prenom LIKE :prenom || '%') " +
            "AND (:email IS NULL OR u.email LIKE :email || '%')")
    Page<Utilisateur> findByFilters(Role role, Long departmentId, String nom, String prenom, String email, Pageable pageable);
}