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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.security.SecureRandom;
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
    private final JavaMailSender mailSender;

    public Utilisateur createUtilisateur(Utilisateur utilisateur, Long departmentId) {
        logger.info("Début de la création d'un utilisateur avec email : {}", utilisateur.getEmail());
        validateUtilisateur(utilisateur, departmentId);

        String rawPassword = utilisateur.getMotDePasse();
        boolean isTempPassword = false;
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = generateTemporaryPassword();
            isTempPassword = true;
        }
        utilisateur.setMotDePasse(passwordEncoder.encode(rawPassword));

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

        if (isTempPassword) {
            sendTemporaryPassword(utilisateur.getEmail(), rawPassword);
        }

        return savedUtilisateur;
    }

    public void sendTemporaryPassword(String to, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Bienvenue sur Symolia - Votre mot de passe temporaire");
            helper.setText(createHtmlEmailTemplate(tempPassword), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    private String createHtmlEmailTemplate(String tempPassword) {
        return String.format("""
            <!DOCTYPE html>
                    <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Bienvenue sur Symolia</title>
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                body {
                    font-family: Arial, sans-serif;
                    line-height: 1.6;
                    color: #333333;
                    background-color: #f8f9fa;
                }
                .email-container {
                    max-width: 600px;
                    margin: 0 auto;
                    background-color: #ffffff;
                    border-radius: 12px;
                    overflow: hidden;
                    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
                }
                .header {
                    background: linear-gradient(135deg,rgb(68, 97, 230) 0%%,rgb(49, 14, 241) 100%%);
                    padding: 40px 20px;
                    text-align: center;
                    color: white;
                }
                .company-name {
                    font-size: 28px;
                    font-weight: 700;
                    margin-bottom: 8px;
                }
                .content {
                    padding: 50px 40px;
                    text-align: center;
                }
                .welcome-title {
                    font-size: 24px;
                    font-weight: 600;
                    color: #2d3748;
                    margin-bottom: 20px;
                }
                .password-section {
                    background: #f7fafc;
                    border: 2px dashed #e2e8f0;
                    border-radius: 12px;
                    padding: 30px;
                    margin: 30px 0;
                }
                .password-value {
                    font-size: 24px;
                    font-weight: 700;
                    color: #2d3748;
                    background: white;
                    padding: 15px 25px;
                    border-radius: 8px;
                    border: 1px solid #e2e8f0;
                    display: inline-block;
                }
                .cta-button {
                    display: inline-block;
                    background: linear-gradient(135deg,rgb(8, 49, 233) 0%%,rgb(46, 0, 250) 100%%);
                    color: white;
                    padding: 16px 32px;
                    text-decoration: none;
                    border-radius: 50px;
                    font-weight: 600;
                    margin: 25px 0;
                }
            </style>
        </head>
        <body>
            <div class="email-container">
                <div class="header">
                    <div class="company-name">SYMOLIA</div>
                    <div>Service Desk</div>
                </div>
                <div class="content">
                    <h1 class="welcome-title">Bienvenue dans votre espace Symolia !</h1>
                    <p>Votre compte a été créé avec succès. Voici votre mot de passe temporaire :</p>
                    <div class="password-section">
                        <div>Votre mot de passe temporaire</div>
                        <div class="password-value">%s</div>
                    </div>
                    <a href="http://localhost:4200/login" class="cta-button">
                        Se connecter maintenant
                    </a>
                    <p>Changez ce mot de passe dès votre première connexion.</p>
                </div>
            </div>
        </body>
        </html>
        """, tempPassword);
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
        if (utilisateur.getMotDePasse() != null && !utilisateur.getMotDePasse().isEmpty()) {
            existingUser.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        }
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


    public Utilisateur updateUtilisateurProfile(Long id, String nom, String prenom, Long departementId) {
        logger.info("Mise à jour du profil utilisateur pour ID: {} - nom: {}, prenom: {}, dept: {}", 
                    id, nom, prenom, departementId);
        
        Optional<Utilisateur> existingUserOpt = utilisateurRepository.findById(id);
        if (existingUserOpt.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé avec ID: " + id);
        }
        
        Utilisateur existingUser = existingUserOpt.get();
        
        String existingPassword = existingUser.getMotDePasse();
        String existingEmail = existingUser.getEmail();
        Role existingRole = existingUser.getRole();
        LocalDateTime existingDateInscription = existingUser.getDateInscription();
        
        existingUser.setNom(nom);
        existingUser.setPrenom(prenom);
        
        existingUser.setMotDePasse(existingPassword); 
        existingUser.setEmail(existingEmail);
        existingUser.setRole(existingRole); 
        existingUser.setDateInscription(existingDateInscription);
        
        if (departementId != null) {
            Departement departement = departementRepository.findById(departementId)
                .orElseThrow(() -> new RuntimeException("Département non trouvé avec ID: " + departementId));
            existingUser.setDepartment(departement);
        }
        
        Utilisateur savedUser = utilisateurRepository.save(existingUser);
        logger.info("Profil utilisateur mis à jour avec succès pour ID: {}", id);
        
        return savedUser;
    }


    public Utilisateur updatePassword(Long userId, String newPassword) {
        logger.info("Mise à jour du mot de passe pour l'utilisateur ID: {}", userId);
        
        Utilisateur existingUser = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec ID: " + userId));
        
        existingUser.setMotDePasse(passwordEncoder.encode(newPassword));
        
        Utilisateur updatedUser = utilisateurRepository.save(existingUser);
        logger.info("Mot de passe mis à jour avec succès pour l'utilisateur ID: {}", userId);
        
        return updatedUser;
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


    public Page<Utilisateur> findByRolePaginated(Role role, Pageable pageable) {
        logger.debug("Recherche d'utilisateurs par rôle paginée: {}", role);
        Page<Utilisateur> utilisateurs = utilisateurRepository.findByRole(role, pageable);
        logger.debug("Recherche réussie, total éléments: {}", utilisateurs.getTotalElements());
        return utilisateurs;
    }


    public List<Utilisateur> findByRoleAndDepartment(Role role, Long departmentId) {
        logger.info("Recherche des utilisateurs avec le rôle {} dans le département {}", role, departmentId);
        return utilisateurRepository.findByRoleAndDepartment_Id(role, departmentId);
    }


    public List<Utilisateur> findTechniciensByUserDepartment(Long userId) {
        logger.debug("Recherche des techniciens du même département que l'utilisateur: {}", userId);
        List<Utilisateur> techniciens = utilisateurRepository.findByRoleAndUserDepartment(Role.TECHNICIEN_SUPPORT, userId);
        logger.debug("Recherche réussie, total éléments: {}", techniciens.size());
        return techniciens;
    }

    private void validateUtilisateur(Utilisateur utilisateur, Long departmentId) {
        logger.debug("Validation de l'utilisateur avec email : {}", utilisateur.getEmail());
        if (utilisateur.getEmail() == null ||
            utilisateur.getNom() == null ||
            utilisateur.getPrenom() == null ||
            utilisateur.getRole() == null) {
            logger.error("Validation échouée : champs obligatoires manquants pour l'email : {}", utilisateur.getEmail());
            throw new IllegalArgumentException("Tous les champs obligatoires doivent être remplis");
        }
        if (utilisateurRepository.existsByEmail(utilisateur.getEmail())) {
            logger.error("Validation échouée : email déjà existant : {}", utilisateur.getEmail());
            throw new IllegalArgumentException("L'email existe déjà");
        }
        logger.debug("Validation réussie pour l'utilisateur avec email : {}", utilisateur.getEmail());
    }

    public long countAll() {
        return utilisateurRepository.count();
    }
    
    public String generateTemporaryPassword() {
        int length = 10;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }


    public boolean existsByEmail(String email) {
        return utilisateurRepository.existsByEmail(email);
    }


    public Utilisateur updateUtilisateurProfileSimple(Long id, String nom, String prenom) {
        logger.info("Mise à jour simplifiée du profil utilisateur pour ID: {} - nom: {}, prenom: {}", 
                    id, nom, prenom);
        
        Optional<Utilisateur> existingUserOpt = utilisateurRepository.findById(id);
        if (existingUserOpt.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé avec ID: " + id);
        }
        
        Utilisateur existingUser = existingUserOpt.get();
        
        String existingPassword = existingUser.getMotDePasse();
        String existingEmail = existingUser.getEmail();
        Role existingRole = existingUser.getRole();
        LocalDateTime existingDateInscription = existingUser.getDateInscription();
        Departement existingDepartment = existingUser.getDepartment();
        
        existingUser.setNom(nom);
        existingUser.setPrenom(prenom);
        
        existingUser.setMotDePasse(existingPassword);
        existingUser.setEmail(existingEmail);
        existingUser.setRole(existingRole);
        existingUser.setDateInscription(existingDateInscription);
        existingUser.setDepartment(existingDepartment);
        
        Utilisateur savedUser = utilisateurRepository.save(existingUser);
        logger.info("Profil utilisateur simplifié mis à jour avec succès pour ID: {}", id);
        
        return savedUser;
    }
}