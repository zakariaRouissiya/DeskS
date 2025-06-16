package com.symolia.DeskS.services;

import com.symolia.DeskS.entities.Departement;
import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.enums.Role;
import com.symolia.DeskS.repositories.DepartementRepository;
import com.symolia.DeskS.repositories.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private static final Logger logger = LoggerFactory.getLogger(UtilisateurService.class);

    private final UtilisateurRepository utilisateurRepository;
    private final DepartementRepository departementRepository;
    private final PasswordEncoder passwordEncoder;

    public Utilisateur createUtilisateur(Utilisateur utilisateur, Long departmentId) {
        logger.info("Début de la création d'un utilisateur avec email : {}", utilisateur.getEmail());
        validateUtilisateur(utilisateur, departmentId);
        utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        if (departmentId != null) {
            Departement departement = departementRepository.findById(departmentId)
                    .orElseThrow(() -> {
                        logger.error("Département non trouvé avec ID : {}", departmentId);
                        return new IllegalArgumentException("Département non trouvé avec ID : " + departmentId);
                    });
            utilisateur.setDepartment(departement);
        }
        utilisateur.setDateInscription(LocalDateTime.now());
        Utilisateur savedUtilisateur = utilisateurRepository.save(utilisateur);
        logger.info("Utilisateur créé avec succès, ID : {}", savedUtilisateur.getId());
        return savedUtilisateur;
    }

    public Page<Utilisateur> getAllUtilisateurs(Pageable pageable, Role role, Long departmentId, String nom, String prenom, String email) {
        nom = (nom != null) ? nom.toLowerCase() : null;
        prenom = (prenom != null) ? prenom.toLowerCase() : null;
        email = (email != null) ? email.toLowerCase() : null;

        return utilisateurRepository.findByFilters(role, departmentId, nom, prenom, email, pageable);
    }

    public Page<Utilisateur> rechercherUtilisateurs(Pageable pageable, String nom, String prenom, String email, Role role, Long departmentId) {
        logger.debug("Recherche d'utilisateurs avec filtres : nom={}, prenom={}, email={}, role={}, departmentId={}",
                nom, prenom, email, role, departmentId);
        Page<Utilisateur> utilisateurs = utilisateurRepository.findByFilters(role, departmentId, nom, prenom, email, pageable);
        logger.debug("Recherche réussie, total éléments : {}", utilisateurs.getTotalElements());
        return utilisateurs;
    }

    public Optional<Utilisateur> getUtilisateurById(Long id) {
        logger.debug("Recherche d'un utilisateur par ID : {}", id);
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(id);
        if (utilisateur.isEmpty()) {
            logger.warn("Utilisateur non trouvé avec ID : {}", id);
        }
        return utilisateur;
    }

    public Utilisateur updateUtilisateur(Long id, Utilisateur utilisateur, Long departmentId) {
        logger.info("Début de la mise à jour de l'utilisateur avec ID : {}", id);
        Utilisateur existingUser = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec ID : " + id));

        existingUser.setEmail(utilisateur.getEmail());
        existingUser.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        existingUser.setNom(utilisateur.getNom());
        existingUser.setPrenom(utilisateur.getPrenom());
        existingUser.setRole(utilisateur.getRole());

        if (departmentId != null) {
            Departement departement = departementRepository.findById(departmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Département non trouvé avec ID : " + departmentId));
            existingUser.setDepartment(departement);
        } else {
            existingUser.setDepartment(null);
        }


        Utilisateur updatedUtilisateur = utilisateurRepository.save(existingUser);
        logger.info("Utilisateur mis à jour avec succès, ID : {}", updatedUtilisateur.getId());
        return updatedUtilisateur;
    }


    public void deleteUtilisateur(Long id) {
        logger.info("Début de la suppression de l'utilisateur avec ID : {}", id);
        if (!utilisateurRepository.existsById(id)) {
            logger.error("Utilisateur non trouvé avec ID : {}", id);
            throw new IllegalArgumentException("Utilisateur non trouvé avec ID : " + id);
        }
        utilisateurRepository.deleteById(id);
        logger.info("Utilisateur supprimé avec succès, ID : {}", id);
    }

    public Optional<Utilisateur> findByEmail(String email) {
        logger.debug("Recherche d'un utilisateur par email : {}", email);
        Optional<Utilisateur> utilisateur = utilisateurRepository.findByEmail(email);
        if (utilisateur.isEmpty()) {
            logger.warn("Utilisateur non trouvé avec email : {}", email);
        }
        return utilisateur;
    }

    public List<Utilisateur> findByRole(Role role) {
        logger.debug("Recherche d'utilisateurs par rôle : {}", role);
        List<Utilisateur> utilisateurs = utilisateurRepository.findByRole(role);
        logger.debug("Recherche réussie, total éléments : {}", utilisateurs.size());
        return utilisateurs;
    }

    private void validateUtilisateur(Utilisateur utilisateur, Long departmentId) {
        logger.debug("Validation de l'utilisateur avec email : {}", utilisateur.getEmail());
        if (utilisateur.getEmail() == null || utilisateur.getMotDePasse() == null ||
                utilisateur.getNom() == null || utilisateur.getPrenom() == null || utilisateur.getRole() == null) {
            logger.error("Validation échouée : champs obligatoires manquants pour l'email : {}", utilisateur.getEmail());
            throw new IllegalArgumentException("Tous les champs obligatoires doivent être remplis");
        }
        if (utilisateurRepository.existsByEmail(utilisateur.getEmail())) {
            logger.error("Validation échouée : email déjà existant : {}", utilisateur.getEmail());
            throw new IllegalArgumentException("L'email existe déjà");
        }
        logger.debug("Validation réussie pour l'utilisateur avec email : {}", utilisateur.getEmail());
    }
}