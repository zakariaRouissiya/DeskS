package com.symolia.DeskS.web;

import com.symolia.DeskS.entities.Departement;
import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.enums.Role;
import com.symolia.DeskS.services.DepartementService;
import com.symolia.DeskS.services.TicketService;
import com.symolia.DeskS.services.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UtilisateurService utilisateurService;
    private final DepartementService departementService;
    private final TicketService ticketService;

    @PostMapping("/creer-utilisateur")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<Map<String, String>> creerUtilisateur(@RequestBody Map<String, String> payload) {
        System.out.println("Payload reçu: " + payload);
        
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(payload.get("email"));
        utilisateur.setMotDePasse(payload.get("motDePasse"));
        utilisateur.setNom(payload.get("nom"));
        utilisateur.setPrenom(payload.get("prenom"));
        utilisateur.setRole(Role.valueOf(payload.get("role").toUpperCase()));
        
        Long departmentId = payload.get("departementId") != null ? Long.parseLong(payload.get("departementId")) : null;
        
        utilisateurService.createUtilisateur(utilisateur, departmentId);

        return ResponseEntity.ok(Map.of("message", "Utilisateur créé avec succès"));
    }

    @GetMapping("/utilisateurs")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Object>> consulterUtilisateurs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String prenom,
            @RequestParam(required = false) String email) {

        Pageable pageable = PageRequest.of(page, size);
        Role enumRole = role != null ? Role.valueOf(role.toUpperCase()) : null;
        Page<Utilisateur> utilisateurs = utilisateurService.getAllUtilisateurs(pageable, enumRole, departmentId, nom, prenom, email);

        var utilisateursDto = utilisateurs.getContent().stream().map(u -> {
            Map<String, Object> utilisateurMap = new HashMap<>();
            utilisateurMap.put("id", u.getId());
            utilisateurMap.put("email", u.getEmail());
            utilisateurMap.put("nom", u.getNom());
            utilisateurMap.put("prenom", u.getPrenom());
            utilisateurMap.put("role", u.getRole().name());
            if (u.getDepartment() != null) {
                utilisateurMap.put("department", Map.of(
                        "id", u.getDepartment().getId(),
                        "nom", u.getDepartment().getNom()
                ));
            }
            return utilisateurMap;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("utilisateurs", utilisateursDto);
        response.put("totalElements", utilisateurs.getTotalElements());
        response.put("totalPages", utilisateurs.getTotalPages());
        response.put("currentPage", page);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/utilisateur/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<Map<String, String>> modifierUtilisateur(
            @PathVariable Long id, @RequestBody Map<String, String> payload) {
        System.out.println("Payload reçu pour modification: " + payload); 
        
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(payload.get("email"));
        if (payload.get("motDePasse") != null && !payload.get("motDePasse").isEmpty()) {
            utilisateur.setMotDePasse(payload.get("motDePasse"));
        }
        utilisateur.setNom(payload.get("nom"));
        utilisateur.setPrenom(payload.get("prenom"));
        utilisateur.setRole(Role.valueOf(payload.get("role").toUpperCase()));
        
        Long departmentId = payload.get("departementId") != null ? Long.parseLong(payload.get("departementId")) : null;

        utilisateurService.updateUtilisateur(id, utilisateur, departmentId);

        return ResponseEntity.ok(Map.of("message", "Utilisateur modifié avec succès"));
    }

    @DeleteMapping("/utilisateur/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<Map<String, String>> supprimerUtilisateur(@PathVariable Long id) {
        utilisateurService.deleteUtilisateur(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé avec succès"));
    }

    @PostMapping("/creer-departement")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<Map<String, String>> creerDepartement(@RequestBody Map<String, String> payload) {
        Departement departement = new Departement();
        departement.setNom(payload.get("nom"));
        departement.setDescription(payload.get("description"));

        departementService.createDepartement(departement);

        return ResponseEntity.ok(Map.of("message", "Département créé avec succès"));
    }

    @GetMapping("/departements")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Object>> consulterDepartements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String nom) {
        
        System.out.println("Récupération des départements - page: " + page + ", size: " + size + ", nom: " + nom);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Departement> departements = departementService.getAllDepartements(pageable, nom);
        
        System.out.println("Nombre de départements trouvés: " + departements.getContent().size());
        
        Map<String, Object> response = new HashMap<>();
        response.put("departements", departements.getContent());
        response.put("totalElements", departements.getTotalElements());
        response.put("totalPages", departements.getTotalPages());
        response.put("currentPage", page);
        
        System.out.println("Réponse envoyée: " + response);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/departement/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<Map<String, String>> modifierDepartement(
            @PathVariable Long id, @RequestBody Map<String, String> payload) {
        Departement departement = new Departement();
        departement.setNom(payload.get("nom"));
        departement.setDescription(payload.get("description"));

        departementService.updateDepartement(id, departement);

        return ResponseEntity.ok(Map.of("message", "Département modifié avec succès"));
    }

    @DeleteMapping("/departement/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<Map<String, String>> supprimerDepartement(@PathVariable Long id) {
        departementService.deleteDepartement(id);
        return ResponseEntity.ok(Map.of("message", "Département supprimé avec succès"));
    }

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", utilisateurService.countAll());
        stats.put("activeTickets", ticketService.countActiveTickets());
        stats.put("openIncidents", ticketService.countOpenIncidents());
        stats.put("closedTickets", ticketService.countClosedTickets());
        return ResponseEntity.ok(stats);
    }
}
