package com.symolia.DeskS.web;

import com.symolia.DeskS.entities.Commentaire;
import com.symolia.DeskS.services.CommentaireService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commentaires")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CommentaireController {

    private static final Logger logger = LoggerFactory.getLogger(CommentaireController.class);
    private final CommentaireService commentaireService;

    @PostMapping("/ticket/{ticketId}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE') or hasAuthority('ROLE_TECHNICIEN_SUPPORT') or hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<Map<String, Object>> createCommentaire(
            @PathVariable Long ticketId,
            @RequestBody Map<String, Object> request) {
        try {
            Long auteurId = Long.valueOf(request.get("auteurId").toString());
            String contenu = request.get("contenu").toString();
            
            logger.info("Création d'un commentaire pour le ticket {} par l'utilisateur {}", ticketId, auteurId);
            
            Commentaire commentaire = commentaireService.createCommentaire(ticketId, auteurId, contenu);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Commentaire ajouté avec succès");
            response.put("commentaire", commentaire);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la création du commentaire: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE') or hasAuthority('ROLE_TECHNICIEN_SUPPORT') or hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<List<Commentaire>> getCommentairesByTicket(@PathVariable Long ticketId) {
        try {
            logger.info("Récupération des commentaires pour le ticket: {}", ticketId);
            
            List<Commentaire> commentaires = commentaireService.getCommentairesByTicketId(ticketId);
            
            return ResponseEntity.ok(commentaires);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des commentaires: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{commentaireId}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE') or hasAuthority('ROLE_TECHNICIEN_SUPPORT') or hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<Map<String, Object>> updateCommentaire(
            @PathVariable Long commentaireId,
            @RequestBody Map<String, Object> request) {
        try {
            Long auteurId = Long.valueOf(request.get("auteurId").toString());
            String nouveauContenu = request.get("contenu").toString();
            
            logger.info("Mise à jour du commentaire {} par l'utilisateur {}", commentaireId, auteurId);
            
            Commentaire commentaire = commentaireService.updateCommentaire(commentaireId, auteurId, nouveauContenu);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Commentaire mis à jour avec succès");
            response.put("commentaire", commentaire);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du commentaire: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{commentaireId}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE') or hasAuthority('ROLE_TECHNICIEN_SUPPORT') or hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<Map<String, Object>> deleteCommentaire(
            @PathVariable Long commentaireId,
            @RequestParam Long auteurId) {
        try {
            logger.info("Suppression du commentaire {} par l'utilisateur {}", commentaireId, auteurId);
            
            commentaireService.deleteCommentaire(commentaireId, auteurId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Commentaire supprimé avec succès");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du commentaire: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{commentaireId}/can-modify")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE') or hasAuthority('ROLE_TECHNICIEN_SUPPORT') or hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<Map<String, Object>> canUserModifyComment(
            @PathVariable Long commentaireId,
            @RequestParam Long userId) {
        try {
            boolean canModify = commentaireService.canUserModifyComment(commentaireId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("canModify", canModify);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification des droits de modification: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{commentaireId}/can-delete")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE') or hasAuthority('ROLE_TECHNICIEN_SUPPORT') or hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<Map<String, Object>> canUserDeleteComment(
            @PathVariable Long commentaireId,
            @RequestParam Long userId) {
        try {
            boolean canDelete = commentaireService.canUserDeleteComment(commentaireId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("canDelete", canDelete);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification des droits de suppression: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/ticket/{ticketId}/count")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE') or hasAuthority('ROLE_TECHNICIEN_SUPPORT') or hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<Map<String, Object>> countCommentairesByTicket(@PathVariable Long ticketId) {
        try {
            long count = commentaireService.countCommentairesByTicket(ticketId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erreur lors du comptage des commentaires: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
}