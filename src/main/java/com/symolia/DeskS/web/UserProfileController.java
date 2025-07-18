package com.symolia.DeskS.web;

import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.services.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);
    private final UtilisateurService utilisateurService;
    private final PasswordEncoder passwordEncoder;


    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            Utilisateur currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utilisateur non authentifié");
            }
            return ResponseEntity.ok(currentUser);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du profil utilisateur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la récupération du profil utilisateur");
        }
    }


    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, Object> userData) {
        try {
            Utilisateur currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Utilisateur non authentifié"));
            }

            if (userData == null || userData.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Aucune donnée fournie"));
            }

            String nom = (String) userData.get("nom");
            String prenom = (String) userData.get("prenom");

            if (nom == null || nom.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Le nom est requis"));
            }
            if (prenom == null || prenom.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Le prénom est requis"));
            }

            logger.info("Mise à jour du profil pour l'utilisateur ID: {} - nom: {}, prenom: {}", 
                        currentUser.getId(), nom, prenom);

            Utilisateur updatedUser = utilisateurService.updateUtilisateurProfileSimple(
                currentUser.getId(), nom.trim(), prenom.trim());
            
            logger.info("Profil utilisateur mis à jour avec succès pour l'ID: {}", currentUser.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Profil mis à jour avec succès",
                "user", updatedUser
            ));
            
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du profil utilisateur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la mise à jour du profil: " + e.getMessage()
                ));
        }
    }


    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> passwordData) {
        try {
            Utilisateur currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Utilisateur non authentifié"));
            }

            if (passwordData == null || passwordData.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Aucune donnée de mot de passe fournie"));
            }

            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            if (currentPassword == null || currentPassword.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Le mot de passe actuel est requis"));
            }
            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Le nouveau mot de passe est requis"));
            }

            logger.info("Tentative de mise à jour du mot de passe pour l'utilisateur ID: {}", currentUser.getId());

            if (!passwordEncoder.matches(currentPassword, currentUser.getMotDePasse())) {
                logger.warn("Mot de passe actuel incorrect pour l'utilisateur ID: {}", currentUser.getId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Mot de passe actuel incorrect"));
            }

            utilisateurService.updatePassword(currentUser.getId(), newPassword);
            
            logger.info("Mot de passe mis à jour avec succès pour l'utilisateur ID: {}", currentUser.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Mot de passe mis à jour avec succès"
            ));
            
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du mot de passe", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false, 
                    "message", "Erreur lors de la mise à jour du mot de passe: " + e.getMessage()
                ));
        }
    }


    private Utilisateur getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            Optional<Utilisateur> userOpt = utilisateurService.findByEmail(auth.getName());
            return userOpt.orElse(null);
        }
        return null;
    }
}