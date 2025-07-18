package com.symolia.DeskS.web;

import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.enums.Role;
import com.symolia.DeskS.services.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UtilisateursController {

    private static final Logger logger = LoggerFactory.getLogger(UtilisateursController.class);
    private final UtilisateurService utilisateurService;

    @GetMapping("/employees")
    public ResponseEntity<List<Utilisateur>> getEmployees() {
        logger.info("Récupération de tous les employés");
        return ResponseEntity.ok(utilisateurService.findByRole(Role.EMPLOYEE));
    }

    @GetMapping("/techniciens")
    public ResponseEntity<List<Utilisateur>> getTechniciens() {
        logger.info("Récupération de tous les techniciens");
        return ResponseEntity.ok(utilisateurService.findByRole(Role.TECHNICIEN_SUPPORT));
    }

    @GetMapping("/managers")
    public ResponseEntity<List<Utilisateur>> getManagers() {
        logger.info("Récupération de tous les managers");
        return ResponseEntity.ok(utilisateurService.findByRole(Role.MANAGER_IT));
    }

    @GetMapping("/administrateurs")
    public ResponseEntity<List<Utilisateur>> getAdministrateurs() {
        logger.info("Récupération de tous les administrateurs");
        return ResponseEntity.ok(utilisateurService.findByRole(Role.ADMINISTRATEUR));
    }

    @GetMapping("/utilisateurs/role/{role}")
    public ResponseEntity<List<Utilisateur>> getUtilisateursByRole(@PathVariable String role) {
        try {
            Role enumRole = Role.valueOf(role);
            logger.info("Récupération des utilisateurs avec le rôle: {}", enumRole);
            return ResponseEntity.ok(utilisateurService.findByRole(enumRole));
        } catch (IllegalArgumentException e) {
            logger.error("Rôle invalide: {}", role, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/utilisateurs/departement/{departmentId}/role/{role}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT') or hasAuthority('ROLE_ADMINISTRATEUR') or hasAuthority('ROLE_TECHNICIEN_SUPPORT')")
    public ResponseEntity<List<Utilisateur>> getUtilisateursByDepartmentAndRole(
            @PathVariable Long departmentId, 
            @PathVariable String role) {
        try {
            logger.info("Récupération des utilisateurs du département {} avec le rôle {}", departmentId, role);
            
            Role enumRole = Role.valueOf(role.toUpperCase());
            List<Utilisateur> utilisateurs = utilisateurService.findByRoleAndDepartment(enumRole, departmentId);
            
            logger.info("Utilisateurs trouvés: {}", utilisateurs.size());
            
            for (Utilisateur user : utilisateurs) {
                logger.info("Utilisateur trouvé: {} {} - Département: {}", 
                    user.getPrenom(), user.getNom(), 
                    user.getDepartment() != null ? user.getDepartment().getNom() : "AUCUN");
            }
            
            return ResponseEntity.ok(utilisateurs);
        } catch (IllegalArgumentException e) {
            logger.error("Rôle invalide: {}", role);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des utilisateurs: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/techniciens/pour-utilisateur/{userId}")
    public ResponseEntity<List<Utilisateur>> getTechniciensForUserDepartment(@PathVariable Long userId) {
        logger.info("Récupération des techniciens pour le département de l'utilisateur: {}", userId);
        try {
            List<Utilisateur> techniciens = utilisateurService.findTechniciensByUserDepartment(userId);
            
            logger.info("Techniciens trouvés: {}", techniciens.size());
            for (Utilisateur tech : techniciens) {
                logger.info("Technicien: {} {} - Département: {}", 
                    tech.getPrenom(), tech.getNom(), 
                    tech.getDepartment() != null ? tech.getDepartment().getNom() : "AUCUN");
            }
            
            return ResponseEntity.ok(techniciens);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des techniciens pour l'utilisateur: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/utilisateurs/pagines")
    public ResponseEntity<Map<String, Object>> getUtilisateursPagines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Utilisateur> utilisateursPage;
            
            if (role != null && !role.isEmpty()) {
                Role enumRole = Role.valueOf(role);
                utilisateursPage = utilisateurService.findByRolePaginated(enumRole, pageable);
            } else {
                utilisateursPage = utilisateurService.getAllUtilisateurs(pageable, null, null, null, null, null);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("utilisateurs", utilisateursPage.getContent());
            response.put("currentPage", utilisateursPage.getNumber());
            response.put("totalItems", utilisateursPage.getTotalElements());
            response.put("totalPages", utilisateursPage.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des utilisateurs paginés", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/utilisateurs/departement/{departmentId}/role/EMPLOYEE")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE') or hasAuthority('ROLE_MANAGER_IT') or hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<List<Utilisateur>> getEmployeesByDepartment(@PathVariable Long departmentId) {
        try {
            logger.info("Récupération des employés du département: {}", departmentId);
            
            List<Utilisateur> employees = utilisateurService.findByRoleAndDepartment(Role.EMPLOYEE, departmentId);
            
            logger.info("Employés trouvés: {}", employees.size());
            
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des employés du département: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}